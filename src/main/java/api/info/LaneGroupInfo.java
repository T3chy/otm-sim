package api.info;

import common.AbstractLaneGroup;

import java.util.*;

public class LaneGroupInfo {

    /** Integer id of the actuator. */
    public long id;

    public float length;
    public float max_vehicles;

    /** List of lane numbers in this lane goup. Lanes are enumerated from
     * inside to outside and include addlanes (turn pockets and lane drops) */
    public List<Integer> lanes;

    /** Map from downstream link id to the list of feasible lane groups. */
//    public Map<Long,List<MacroLaneGroupInfo>> outlink2roadconnection = new HashMap<>()

    /** Id for an actuator on this lane group. */
    public Long actuator_id;

    public LaneGroupInfo(AbstractLaneGroup x){
        this.id = x.id;
        this.length = x.length;
        this.lanes = x.get_dn_lanes();
        this.actuator_id = x.actuator_capacity==null ? null : x.actuator_capacity.id;
//        for(Map.Entry e : x.outlink2roadconnection.entrySet()){
//            Set<AbstractLaneGroup> nextlanegroups = (Set<AbstractLaneGroup>) e.getValue();
//            List<MacroLaneGroupInfo> s = new ArrayList<>();
//            nextlanegroups.forEach( nlg -> s.add(new MacroLaneGroupInfo(nlg)));
//            outlink2nextlanegroups.put((Long) e.getKey(),s);
//        }
    }

    public long getId() {
        return id;
    }

    public float getLength() {
        return length;
    }

    public float getMax_vehicles() {
        return max_vehicles;
    }

    public List<Integer> getLanes() {
        return lanes;
    }

    public Long getActuator_id() {
        return actuator_id;
    }

    @Override
    public String toString() {
        return "MacroLaneGroupInfo{" +
                "id=" + id +
                ", length=" + length +
                ", max_vehicles=" + max_vehicles +
                ", lanes=" + lanes +
//                ", outlink2nextlanegroups=" + outlink2nextlanegroups +
                ", actuator_id=" + actuator_id +
                '}';
    }
}
