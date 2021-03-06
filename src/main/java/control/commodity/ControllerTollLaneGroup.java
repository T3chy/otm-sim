package control.commodity;

import actuator.ActuatorOpenCloseLaneGroup;
import common.AbstractLaneGroup;
import common.FlowAccumulatorState;
import common.LaneGroupSet;
import common.Scenario;
import control.AbstractController;
import control.command.CommandRestrictionMap;
import dispatch.Dispatcher;
import dispatch.EventPoke;
import error.OTMException;
import jaxb.Controller;
import lanechange.AbstractLaneSelector;
import lanechange.LogitLaneSelector;
import models.fluid.AbstractFluidModel;
import models.fluid.FluidLaneGroup;
import utils.OTMUtils;
import utils.LookupTable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ControllerTollLaneGroup extends AbstractController {

    public boolean firsttime;
    public final float def_keep = 0.7f;
    public final float def_rho_vpkmplane = 0.007147f;
    public Set<Long> free_comms = new HashSet<>();
    public Set<Long> banned_comms = new HashSet<>();
    public Set<Long> tolled_comms = new HashSet<>();
    public float toll_coef;
    public float speed_threshold_meterpdt;
    public LookupTable vplpdt_to_cents_table;

    public Set<LGInfo> lginfos;

    public ControllerTollLaneGroup(Scenario scenario, Controller jcnt) throws OTMException {
        super(scenario, jcnt);

        if(jcnt.getParameters()!=null)
            for(jaxb.Parameter p : jcnt.getParameters().getParameter()){
                switch(p.getName()){
                    case "free_comms":
                        free_comms.addAll(OTMUtils.csv2longlist(p.getValue()));
                        break;
                    case "disallowed_comms":
                        banned_comms.addAll(OTMUtils.csv2longlist(p.getValue()));
                        break;
                    case "tolled_comms":
                        tolled_comms.addAll(OTMUtils.csv2longlist(p.getValue()));
                        break;
                    case "vplph_to_cents_table":
                        vplpdt_to_cents_table = new LookupTable(p.getValue());
                        vplpdt_to_cents_table.scaleX(dt/3600f);
                        break;
                    case "toll_coef":
                        toll_coef = Float.parseFloat(p.getValue());
                        break;
                    case "qos_speed_threshold_kph":
                        speed_threshold_meterpdt = Float.parseFloat(p.getValue()); // fix units in initialize
                        speed_threshold_meterpdt *= 1000.0*dt/3600.0;
                        break;
                }
            }
    }

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);
        this.firsttime=true;

        ActuatorOpenCloseLaneGroup act = (ActuatorOpenCloseLaneGroup)actuators.values().iterator().next();
        this.lginfos = new HashSet<>();
        for (AbstractLaneGroup hotlg : ((LaneGroupSet) act.target).lgs)
            lginfos.add(new LGInfo(hotlg));
    }

    @Override
    public void update_command(Dispatcher dispatcher) throws OTMException {

        float timestamp = dispatcher.current_time;
        long act_id = this.actuators.keySet().iterator().next();

        if(firsttime){

            // open/close the lanegroup
            Map<Long, ControllerRestrictLaneGroup.Restriction> X = new HashMap<>();
            for (Long commid : free_comms)
                X.put(commid, ControllerRestrictLaneGroup.Restriction.Open);
            for (Long commid : tolled_comms)
                X.put(commid, ControllerRestrictLaneGroup.Restriction.Open);
            for (Long commid : banned_comms)
                X.put(commid, ControllerRestrictLaneGroup.Restriction.Closed);
            command.put(act_id,new CommandRestrictionMap(X));

            // save the existing lane selector and replace it with a new one
            lginfos.forEach(l->l.initialize(dispatcher));

            firsttime=false;
        }

        else if(timestamp<end_time)
            lginfos.forEach(l->l.update());

        // final time, return to previous state
        else {

            // release restrictions
            Map<Long, ControllerRestrictLaneGroup.Restriction> X = new HashMap<>();
            for(Long commid : this.scenario.commodities.keySet())
                X.put(commid, ControllerRestrictLaneGroup.Restriction.Open);
            command.put(act_id,new CommandRestrictionMap(X));

            // put lane selector backs
            lginfos.forEach(l->l.restore(scenario));
        }

        // register next poke
//        if (timestamp<end_time)
//            dispatcher.register_event(new EventPoke(dispatcher,19,this.end_time,this));

    }

    class LGInfo {
        AbstractLaneGroup gplg;
        AbstractLaneGroup hotlg;
        FlowAccumulatorState fa;
        double ffspeed_meterperdt;
        Map<Long,AbstractLaneSelector> nom_ls;
        Map<Long,LogitLaneSelector> toll_ls;
        double prev_count;

        public LGInfo(AbstractLaneGroup hotlg){
            this.hotlg = hotlg;
            this.gplg = hotlg.neighbor_out;
            this.fa = hotlg.request_flow_accumulator(null);
            prev_count = fa.get_total_count();
        }

        public void initialize(Dispatcher dispatcher){
            nom_ls = new HashMap<>();
            toll_ls = new HashMap<>();

            int numcells =  ((FluidLaneGroup)hotlg).cells.size();
            double celllength_meter = hotlg.length / numcells;
            ffspeed_meterperdt = ((FluidLaneGroup)hotlg).ffspeed_cell_per_dt * celllength_meter;
            ffspeed_meterperdt *= dt/((AbstractFluidModel)hotlg.link.model).dt_sec;

            for(Long commid : tolled_comms){
                LogitLaneSelector newls;
                if(gplg.lane_selector.containsKey(commid)) {
                    AbstractLaneSelector oldls =  gplg.lane_selector.get(commid);

                    // remove future pokes
                    dispatcher.remove_events_for_recipient(EventPoke.class,oldls);

                    nom_ls.put(commid,oldls);
                    if(oldls instanceof LogitLaneSelector) {
                        LogitLaneSelector oldlogit = (LogitLaneSelector) oldls;
                        newls = new LogitLaneSelector(gplg,0,(float)oldlogit.getKeep(),(float)oldlogit.getRho_vehperlane(), commid);
                    }
                    else
                        newls = new LogitLaneSelector(gplg,0,def_keep,def_rho_vpkmplane, commid);
                }
                else
                    newls = new LogitLaneSelector(gplg,0,def_keep,def_rho_vpkmplane, commid);
                try {
                    newls.initialize(scenario,start_time);
                } catch (OTMException e) {
                    e.printStackTrace();
                }
                toll_ls.put(commid,newls);
                gplg.lane_selector.put(commid,newls);
            }
        }

        public void restore(Scenario scenario){
            try {
                for(Long commid : tolled_comms) {
                    if (nom_ls.containsKey(commid)) {
                        AbstractLaneSelector old_ls = nom_ls.get(commid);
                        old_ls.initialize(scenario,scenario.dispatcher.current_time);
                        gplg.lane_selector.put(commid, old_ls);
                    }

                    // remove future pokes for generated lane selectors
                    scenario.dispatcher.remove_events_for_recipient(EventPoke.class,toll_ls.get(commid));
                }
            } catch (OTMException e) {
                e.printStackTrace();
            }
        }

        public void update(){
            double count = fa.get_total_count();
            double flow_vpdt = count-prev_count;
            prev_count = count;

            double veh = hotlg.get_total_vehicles();

            double speed_meterperdt = veh<1 ? ffspeed_meterperdt : hotlg.length*flow_vpdt/veh;
            if(speed_meterperdt>ffspeed_meterperdt)
                speed_meterperdt = ffspeed_meterperdt;

            double add_term;
            if(speed_meterperdt > speed_threshold_meterpdt)
                add_term = Double.POSITIVE_INFINITY;
            else {
                double toll = vplpdt_to_cents_table.get_value_for((float)flow_vpdt/hotlg.num_lanes);
                add_term = toll_coef*toll;
            }
            for(Long commid : tolled_comms)
                toll_ls.get(commid).setAdd_in(add_term);
        }

    }

}
