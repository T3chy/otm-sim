package models.vehicle;

import common.AbstractVehicle;
import common.Link;
import common.RoadConnection;
import geometry.FlowPosition;
import geometry.Side;
import common.AbstractLaneGroup;
import jaxb.Roadparam;
import models.vehicle.newell.ModelNewell;
import packet.*;

import java.util.HashSet;
import java.util.Set;

public abstract class VehicleLaneGroup extends AbstractLaneGroup {

    public double max_vehicles;

    ////////////////////////////////////////
    // construction
    ////////////////////////////////////////

    public VehicleLaneGroup(Link link, Side side, FlowPosition flwpos, float length, int num_lanes, int start_lane, Set<RoadConnection> out_rcs, jaxb.Roadparam rp) {
        super(link, side, flwpos, length, num_lanes, start_lane, out_rcs, rp);
    }

    ////////////////////////////////////////
    // InterfaceLaneGroup
    ////////////////////////////////////////


    @Override
    public void set_road_params(Roadparam r) {
        this.max_vehicles =  r.getJamDensity() * (length/1000.0) * num_lanes;
    }

    @Override
    public float vehs_out_for_comm(Long comm_id) {
        return 0f;
    }

    @Override
    public double get_max_vehicles() {
        return max_vehicles;
    }

//    @Override
//    public void exiting_roadconnection_capacity_has_been_modified(float timestamp) {
//        System.err.println("NOT IMPLEMENTED");
//    }

    @Override
    public float vehs_in_for_comm(Long comm_id) {
        return 0f;
    }

    ////////////////////////////////////////
    // helper methods
    ////////////////////////////////////////

    protected Set<AbstractVehicle> create_vehicles_from_packet(PacketLaneGroup vp,Long next_link_id) {
        // + The packet received here can be fluid or vehicle based. It will not be both
        // because LaneGroupPackets are already model specific, in the sense of either
        // fluid or vehicle based
        // + All of the keys in the packet should be updated using next_link_id.

        assert(vp.vehicles.size()<2);
        assert(vp.vehicles.isEmpty() || vp.container.isEmpty());

        Set<AbstractVehicle> vehs = new HashSet<>();

        AbstractVehicleModel model = (AbstractVehicleModel) link.model;

        // process 'vehicle' part
        if(!vp.vehicles.isEmpty())
            for(AbstractVehicle abs_veh : vp.vehicles)
                vehs.add(model.translate_vehicle(abs_veh));

        // process 'fluid' part
        if(buffer!=null && !vp.container.isEmpty())
            vehs.addAll( buffer.add_packet_and_extract_vehicles(vp.container,this) );

        // set next link id
        vehs.forEach(v->v.set_next_link_id(next_link_id));

        return vehs;
    }

}
