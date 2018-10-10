/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package models.ctm;

import error.OTMErrorLog;
import keys.KeyCommPathOrLink;
import utils.OTMUtils;

import java.util.HashMap;
import java.util.Map;

public class Cell {

    public LinkModel model;
    private LaneGroup laneGroup;

    public boolean am_upstrm;
    public boolean am_dnstrm;

    // road params
    public double capacity_veh;         // [veh]
    public double wspeed_norm;          // [-]
    public double ffspeed_norm;         // [-]
    public double jam_density_veh;      // [veh]

    // vehicles and demand already in their target lanegroup
    public Map<KeyCommPathOrLink, Double> veh_dwn;      // comm,path|nlink -> number of vehicles
    public Map<KeyCommPathOrLink, Double> demand_dwn;   // comm,path|nlink -> number of vehicles
    public double total_vehs_dwn;

    // vehicles changing lanes outward
    public Map<KeyCommPathOrLink, Double> veh_out;      // comm,path|nlink -> number of vehicles
    public Map<KeyCommPathOrLink, Double> demand_out;   // comm,path|nlink -> number of vehicles
    public double total_vehs_out;

    // vehicles changing lanes inward
    public Map<KeyCommPathOrLink, Double> veh_in;      // comm,path|nlink -> number of vehicles
    public Map<KeyCommPathOrLink, Double> demand_in;   // comm,path|nlink -> number of vehicles
    public double total_vehs_in;

    public double supply;   // [veh]

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public Cell(LinkModel model, double length_in_meters, LaneGroup laneGroup) {
        this.am_upstrm = false;
        this.am_dnstrm = false;
        this.model = model;
        this.laneGroup = laneGroup;
    }

    public void set_road_params(float capacity_vehperlane, float jam_density_vehperlane, float ffspeed_veh) {
        int lanes = laneGroup.num_lanes;
        if (model.link.is_source) {
            this.capacity_veh = capacity_vehperlane * lanes;
            this.ffspeed_norm = Double.NaN;
            this.jam_density_veh = Double.NaN;
            this.wspeed_norm = Double.NaN;
        } else {
            this.capacity_veh = capacity_vehperlane * lanes;
            this.ffspeed_norm = ffspeed_veh;
            this.jam_density_veh = jam_density_vehperlane * lanes;
            double critical_veh = capacity_veh / ffspeed_norm;
            this.wspeed_norm = capacity_veh / (jam_density_veh - critical_veh);
        }
    }

    public void validate(OTMErrorLog errorLog) {

        if (!model.link.is_source) {
            if (ffspeed_norm < 0)
                errorLog.addError("non-negativity");
            if (jam_density_veh < 0)
                errorLog.addError("non-negativity");
            if (wspeed_norm < 0)
                errorLog.addError("non-negativity");
            if (wspeed_norm > 1)
                errorLog.addError("CFL violated: link " + laneGroup.link.getId() + " wspeed_norm = " + wspeed_norm);
            if (ffspeed_norm > 1)
                errorLog.addError("CFL violated: link " + laneGroup.link.getId() + " ffspeed_norm = " + ffspeed_norm);
        }

        // NOTE: THIS IS FAILING BECAUSE LANES HAVE NOT BEEN SET
//        if(capacity_veh<=0)
//            scenario.error_log.addError("capacity must be positive");
    }

    public void reset(){

    }

    public void allocate_state() {

        // downstream flow
        veh_dwn = new HashMap<>();
        demand_dwn = new HashMap<>();
        for (KeyCommPathOrLink k : laneGroup.states) {
            veh_dwn.put(k, 0d);
            demand_dwn.put(k, 0d);
        }

        // outward flow
        if (laneGroup.neighbor_out != null) {
            veh_out = new HashMap<>();
            demand_out = new HashMap<>();
            for (KeyCommPathOrLink k : laneGroup.neighbor_out.states) {
                veh_out.put(k, 0d);
                demand_out.put(k, 0d);
            }
        }

        // inward flow
        if (laneGroup.neighbor_in != null) {
            veh_in = new HashMap<>();
            demand_in = new HashMap<>();
            for (KeyCommPathOrLink k : laneGroup.neighbor_in.states) {
                veh_in.put(k, 0d);
                demand_in.put(k, 0d);
            }
        }

    }

    ///////////////////////////////////////////////////
    // get
    ///////////////////////////////////////////////////

    public double get_vehicles() {
        return total_vehs_dwn + total_vehs_in + total_vehs_out;
    }

    public double get_veh_dwn_for_commodity(Long commodity_id) {

        if (commodity_id == null)
            return get_vehicles();

        return veh_dwn.entrySet().stream()
                .filter(x->x.getKey().commodity_id==commodity_id)
                .mapToDouble(x->x.getValue())
                .sum();
    }

    ///////////////////////////////////////////////////
    // update
    ///////////////////////////////////////////////////

    // compute demand_dwn (demand per commodity and path)
    // and supply (total supply)
    public void update_supply_demand() {

        double total_vehicles = total_vehs_dwn + total_vehs_out + total_vehs_in;

        double external_max_speed = Double.POSITIVE_INFINITY;
        double total_demand;

        // update demand ...................................................

        // case empty link
        if (total_vehicles < OTMUtils.epsilon) {
            demand_dwn.keySet().stream().forEach(k -> demand_dwn.put(k, 0d));
            if(demand_out!=null)
                demand_out.keySet().stream().forEach(k -> demand_out.put(k, 0d));
            if(demand_in!=null)
                demand_in.keySet().stream().forEach(k -> demand_in.put(k, 0d));
        }

        else {

            // compute total flow leaving the cell in the absence of flow control
            if (model.link.is_source)
                // sources discharge at capacity
                total_demand = Math.min(total_vehicles, capacity_veh);
            else {
                // assume speed control acts equally on all cells in the link
                double ffspeed = Math.min(ffspeed_norm, external_max_speed);
                if(am_dnstrm)
                    total_demand = Math.min(ffspeed * total_vehicles, capacity_veh);
                else
                    total_demand = ffspeed * total_vehicles;
            }

            // downstream cell: flow controller and lane change blocking
            if (am_dnstrm) {

                if(total_vehs_out+total_vehs_in>OTMUtils.epsilon) {
//                    double gamma = 0.9d;
//                    double mulitplier = Math.max(0d,1d-gamma*total_vehs_out);
//                    total_demand *= mulitplier;
                    total_demand = 0d;
                }
            }

            // split among in|out target, commodities, paths|nextlinks
            double alpha = total_demand / total_vehicles;
            for (KeyCommPathOrLink key : demand_dwn.keySet())
                demand_dwn.put(key, veh_dwn.get(key) * alpha);

            if(demand_out !=null)
                for (KeyCommPathOrLink key : demand_out.keySet())
                    demand_out.put(key, veh_out.get(key) * alpha);

            if(demand_in !=null)
                for (KeyCommPathOrLink key : demand_in.keySet())
                    demand_in.put(key, veh_in.get(key) * alpha);
        }

        // update supply ..............................................
        if (model.link.is_sink)
            supply = capacity_veh;
        else {
            switch (model.link.model_type) {
                case ctm:
                    if(am_dnstrm)
                        supply = Math.min(wspeed_norm * (jam_density_veh - total_vehicles), capacity_veh);
                    else
                        supply = wspeed_norm * (jam_density_veh - total_vehicles);
                    break;
                case mn:
                    supply = Float.POSITIVE_INFINITY;
                    break;
                default:
                    System.err.println("Wha??");
            }
        }
    }

    public void update_dwn_state(Map<KeyCommPathOrLink, Double> inflow, Map<KeyCommPathOrLink, Double> outflow) {

        if (inflow != null)
            for (Map.Entry<KeyCommPathOrLink, Double> e : inflow.entrySet()) {
                KeyCommPathOrLink state = e.getKey();
                double value = e.getValue();
                if(value>0d) {
                    veh_dwn.put(state, veh_dwn.get(state) + value);
                    total_vehs_dwn += value;
                }
            }

        if (outflow != null)
            for (Map.Entry<KeyCommPathOrLink, Double> e : outflow.entrySet()) {
                KeyCommPathOrLink state = e.getKey();
                double value = e.getValue();
                if(value>0d) {
                    veh_dwn.put(state, veh_dwn.get(state) - value);
                    total_vehs_dwn -= value;
                }
            }

    }

    public void update_out_state(Map<KeyCommPathOrLink, Double> inflow, Map<KeyCommPathOrLink, Double> outflow) {

        if (inflow != null)
            for (Map.Entry<KeyCommPathOrLink, Double> e : inflow.entrySet()) {
                KeyCommPathOrLink state = e.getKey();
                double value = e.getValue();
                if(value>0d) {
                    veh_out.put(state, veh_out.get(state) + value);
                    total_vehs_out += value;
                }
            }

        if (outflow != null)
            for (Map.Entry<KeyCommPathOrLink, Double> e : outflow.entrySet()) {
                KeyCommPathOrLink state = e.getKey();
                double value = e.getValue();
                if(value>0d) {
                    veh_out.put(state, veh_out.get(state) - value);
                    total_vehs_out -= value;
                }
            }

         if(veh_out==null)
             total_vehs_out = 0d;
    }

    public void update_in_state(Map<KeyCommPathOrLink, Double> inflow, Map<KeyCommPathOrLink, Double> outflow) {

        if (inflow != null)
            for (Map.Entry<KeyCommPathOrLink, Double> e : inflow.entrySet()) {
                KeyCommPathOrLink state = e.getKey();
                double value = e.getValue();
                if(value>0d) {
                    veh_in.put(state, veh_in.get(state) + value);
                    total_vehs_in += value;
                }
            }

        if (outflow != null)
            for (Map.Entry<KeyCommPathOrLink, Double> e : outflow.entrySet()) {
                KeyCommPathOrLink state = e.getKey();
                double value = e.getValue();
                if(value>0d) {
                    veh_in.put(state, veh_in.get(state) - value);
                    total_vehs_in -= value;
                }
            }

        if(veh_in==null)
            total_vehs_in = 0d;

    }

}