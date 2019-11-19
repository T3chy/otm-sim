package api;

import actuator.AbstractActuator;
import actuator.sigint.ActuatorSignal;
import api.info.*;
import commodity.Commodity;
import commodity.Subnetwork;
import common.Link;
import common.Node;
import common.RoadConnection;
import control.AbstractController;
import dispatch.EventCreateVehicle;
import dispatch.EventDemandChange;
import error.OTMErrorLog;
import error.OTMException;
import keys.DemandType;
import keys.KeyCommodityDemandTypeId;
import models.AbstractLaneGroup;
import profiles.AbstractDemandProfile;
import profiles.DemandProfile;
import sensor.AbstractSensor;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

/**
 * Methods for querying and modifying a scenario.
 */
public class Scenario {

    private api.OTM myapi;

    protected Scenario(api.OTM myapi){
        this.myapi = myapi;
    }

    /**
     * Get scenario information.
     * @return a ScenarioInfo object.
     * @see ScenarioInfo
     */
    public ScenarioInfo get_info(){
        return myapi.scn !=null ? new ScenarioInfo(myapi.scn) : null;
    }

    ////////////////////////////////////////////////////////
    // models
    ////////////////////////////////////////////////////////

    /**
     * Get model coverage.
     * @return a set of model info objects
     * @see ModelInfo
     */
    public Set<ModelInfo> get_models(){
        return myapi.scn.network.models.values().stream()
                .map(model->new ModelInfo(model))
                .collect(toSet());
    }

    ////////////////////////////////////////////////////////
    // commodities
    ////////////////////////////////////////////////////////

    /**
     * Get the total number of commodities in the scenario.
     * @return integer number of commodities.
     */
    public int get_num_commodities(){
        return myapi.scn.commodities.size();
    }

    /**
     * Get information for all commodities in the scenario.
     * @return Set of commodity info
     * @see CommodityInfo
     */
    public Set<CommodityInfo> get_commodities(){
        Set<CommodityInfo> commInfo = new HashSet<>();
        for(Commodity comm : myapi.scn.commodities.values())
            commInfo.add(new CommodityInfo(comm));
        return commInfo;
    }

    /**
     * Get information for a specific commodity.
     * @param  id Integer commodity id
     * @return A CommodityInfo object
     * @see CommodityInfo
     */
    public CommodityInfo get_commodity_with_id(long id){
        Commodity comm = myapi.scn.commodities.get(id);
        return comm==null? null : new CommodityInfo(comm);
    }

    /**
     * Get commodity ids.
     * @return set of commodity ids
     */
    public Set<Long> get_commodity_ids(){
        return new HashSet(myapi.scn.commodities.keySet());
    }

    ////////////////////////////////////////////////////////
    // subnetworks and paths
    ////////////////////////////////////////////////////////

    /**
     * Get the total number of subnetworks in the scenario.
     * @return integer number of subnetworks
     */
    public int get_num_subnetworks(){
        return myapi.scn.subnetworks.size();
    }

    /**
     * Get subnetwork ids
     * @return Set of subnetwork ids
     */
    public Set<Long> get_subnetwork_ids(){
        return new HashSet(myapi.scn.subnetworks.keySet());
    }

    /**
     * Get set of all path ids (ie linear subnetworks that begin at a source)
     * @return Set of path ids
     */
    public Set<Long> get_path_ids(){
        return myapi.scn.subnetworks.values().stream()
                .filter(x->x.is_path)
                .map(x->x.getId())
                .collect(toSet());
    }

    /**
     * Get information for all subnetworks in the scenario.
     * @return a set of SubnetworkInfo
     * @see SubnetworkInfo
     */
    public Set<SubnetworkInfo> get_subnetworks(){
        Set<SubnetworkInfo> subnetInfo = new HashSet<>();
        for(Subnetwork subnet : myapi.scn.subnetworks.values())
            subnetInfo.add(new SubnetworkInfo(subnet));
        return subnetInfo;
    }

    /**
     * Get information for a specific subnetwork.
     * @param  id Integer subnetwork id
     * @return A SubnetworkInfo object
     * @see SubnetworkInfo
     */
    public SubnetworkInfo get_subnetwork_with_id(long id){
        Subnetwork subnet = myapi.scn.subnetworks.get(id);
        return subnet==null? null : new SubnetworkInfo(subnet);
    }

    ////////////////////////////////////////////////////////
    // network
    ////////////////////////////////////////////////////////

    /**
     * Get the total number of links in the scenario.
     * @return an integer.
     */
    public int get_num_links(){
        return myapi.scn.network.links.size();
    }

    /**
     * Get the total number of nodes in the scenario.
     * @return an integer.
     */
    public int get_num_nodes(){
        return myapi.scn.network.nodes.size();
    }

    /**
     * Get node ids
     * @return Set of node ids.
     */
    public Set<Long> get_node_ids(){
        return new HashSet(myapi.scn.network.nodes.keySet());
    }

    /**
     * Get information for all nodes in the scenario.
     * @return Map from node id to NodeInfo
     * @see NodeInfo
     */
    public Map<Long, NodeInfo> get_nodes(){
        Map<Long,NodeInfo> nodeInfo = new HashMap<>();
        for(Node node : myapi.scn.network.nodes.values())
            nodeInfo.put(node.getId(),new NodeInfo(node));
        return nodeInfo;
    }

    /**
     * Get information for a specific node.
     * @param node_id Id of the requested node.
     * @return a NodeInfo object
     * @see NodeInfo
     */
    public NodeInfo get_node_with_id(long node_id){
        Node node = myapi.scn.network.nodes.get(node_id);
        return node==null ? null : new NodeInfo(node);
    }

    /**
     * Returns a set where every entry is a list with entries [link_id,start_node,end_node]
     * @return A set of lists
     */
    public Set<List<Long>> get_link_connectivity(){
        Set<List<Long>> X = new HashSet<>();
        for(Link link : myapi.scn.network.links.values()){
            List<Long> A = new ArrayList<>();
            A.add(link.getId());
            A.add(link.start_node.getId());
            A.add(link.end_node.getId());
            X.add(A);
        }
        return X;
    }

    /**
     * Get information for all links in the scenario.
     * @return Map from link id to LinkInfo
     * @see LinkInfo
     */
    public Map<Long,LinkInfo> get_links(){
        return myapi.scn.network.links.values().stream().collect(Collectors.toMap(x->x.getId(),x->new LinkInfo(x)));
    }

    /**
     * Get information for a specific link.
     * @param link_id Id of the requested link.
     * @return a LinkInfo object
     * @see LinkInfo
     */
    public LinkInfo get_link_with_id(long link_id){
        Link link = myapi.scn.network.links.get(link_id);
        return link==null ? null : new LinkInfo(link);
    }

    /**
     * Get the set of ids of all links in the network.
     * @return A set of ids
     */
    public Set<Long> get_link_ids(){
        return new HashSet(myapi.scn.network.links.keySet());
    }

    /**
     * Get ids for all source links.
     * @return A set of ids.
     */
    public Set<Long> get_source_link_ids(){
        return myapi.scn.network.links.values().stream()
                .filter(x->x.is_source)
                .map(x->x.getId())
                .collect(toSet());
    }

    /**
     * Get the lane groups that enter a given road connection
     * @param rcid Id of the road connection
     * @return A set of lane group ids.
     */
    public Set<Long> get_in_lanegroups_for_road_connection(long rcid){
        RoadConnection rc = myapi.scn.network.get_road_connection(rcid);
        Set<Long> lgids = new HashSet<>();
        for(AbstractLaneGroup lg : rc.in_lanegroups)
            lgids.add(lg.id);
        return lgids;
    }

    /**
     * Get the lane groups that exit a given road connection
     * @param rcid Id of the road connection
     * @return A set of lane group ids.
     */
    public Set<Long> get_out_lanegroups_for_road_connection(long rcid){
        RoadConnection rc = myapi.scn.network.get_road_connection(rcid);
        Set<Long> lgids = new HashSet<>();
        for(AbstractLaneGroup lg : rc.out_lanegroups)
            lgids.add(lg.id);
        return lgids;
    }

    /**
     * Get lane groups in every link
     * @return A map from link ids to a set of lane group ids.
     */
    public Map<Long,Set<Long>> get_link2lgs(){
        Map<Long,Set<Long>> lk2lgs = new HashMap<>();
        for(Link link : myapi.scn.network.links.values())
            lk2lgs.put(link.getId(),link.lanegroups_flwdn.values().stream()
                    .map(x->x.id).collect(toSet()));
        return lk2lgs;
    }

    ////////////////////////////////////////////////////////
    // demands / splits
    ////////////////////////////////////////////////////////

    /**
     * Get information for all demands in the scenario.
     * @return A set of DemandInfo
     * @see DemandInfo
     */
    public Set<DemandInfo> get_demands(){
        Set<DemandInfo> x = new HashSet<>();
        for(AbstractDemandProfile y : myapi.scn.data_demands.values())
            x.add(new DemandInfo(y));
        return x;
    }

    /**
     * Get information for a specific demand.
     * @param typestr 'pathfull' or 'pathless' (NOTE: Why is this an input???)
     * @param link_or_path_id Id of the source link or path
     * @param commodity_id Id of the commodity
     * @return A DemandInfo object
     * @see DemandInfo
     */
    public DemandInfo get_demand_with_ids(String typestr,long link_or_path_id,long commodity_id){
        DemandType type = DemandType.valueOf(typestr);
        if(type==null)
            return null;
        AbstractDemandProfile dp = myapi.scn.data_demands.get(new KeyCommodityDemandTypeId(commodity_id,link_or_path_id,type));
        return dp==null ? null : new DemandInfo(dp);
    }

    /**
     *  Clear all demands in the scenario.
     */
    public void clear_all_demands(){

        if(myapi.scn ==null)
            return;

        // delete sources from links
        for(Link link : myapi.scn.network.links.values())
            link.sources = new HashSet<>();

        // delete all EventCreateVehicle and EventDemandChange from dispatcher
        if(myapi.scn.dispatcher!=null) {
            myapi.scn.dispatcher.remove_events_for_recipient(EventCreateVehicle.class);
            myapi.scn.dispatcher.remove_events_for_recipient(EventDemandChange.class);
        }

        // delete all demand profiles
        if(myapi.scn.data_demands!=null)
            myapi.scn.data_demands.clear();
    }

    /**
     * Set or override a demand value for a path.
     * Use this method to set a demand profile of a given commodity on a given path.
     * The profile is a piecewise continuous function starting a time "start_time" and with
     * sample time "dt". The values are given by the "values" array. The value before
     * before "start_time" is zero, and the last value in the array is held into positive
     * infinity time.
     * This method will override any existing demands for that commodity and path.
     *
     * @param path_id : [long] integer id of the subnetwork
     * @param commodity_id : [long] integer id of the commodity
     * @param start_time : [float] start time for the demand profile in seconds after midnight.
     * @param dt : [float] step time for the profile in seconds.
     * @param values : [array of doubles] list of values for the piecewise continuous profile.
     * @throws OTMException Undocumented
     */
    public void set_demand_on_path_in_vph(long path_id,long commodity_id,float start_time,float dt,List<Double> values) throws OTMException {

        // create a demand profile
        Subnetwork path = myapi.scn.subnetworks.get(path_id);
        if(path==null)
            throw new OTMException("Bad path id");

        Commodity commodity = myapi.scn.commodities.get(commodity_id);
        if(commodity==null)
            throw new OTMException("Bad commodity id");

        DemandProfile dp = new DemandProfile(path,commodity,start_time, dt, values);

        // validate
        OTMErrorLog errorLog = new OTMErrorLog();
        dp.validate(errorLog);
        if (errorLog.haserror())
            throw new OTMException(errorLog.format_errors());

        // if a similar demand already exists, then delete it
        if(myapi.scn.data_demands.containsKey(dp.get_key())){
            AbstractDemandProfile old_dp = myapi.scn.data_demands.get(dp.get_key());
            if(myapi.dispatcher!=null)
                myapi.dispatcher.remove_events_for_recipient(EventDemandChange.class,old_dp);
            myapi.scn.data_demands.remove(dp.get_key());

            // remove it and its source from the link
            old_dp.source.link.sources.remove(old_dp.source);
        }

        // add to scenario
        myapi.scn.data_demands.put(dp.get_key(),dp);

        if(myapi.scn.is_initialized) {
            // initialize
            dp.initialize(myapi.scn);

            // send to dispatcher
            dp.register_with_dispatcher(myapi.scn.dispatcher);
        }

    }

    /**
     * Get OD matrix information for this scenario
     * @return List of ODInfo objects
     * @throws OTMException Undocumented
     */
    public List<ODInfo> get_od_info() throws OTMException {

        Map<ODPair,ODInfo> odmap = new HashMap<>();

        for(AbstractDemandProfile demand_profile : myapi.scn.data_demands.values()){

            if(demand_profile.get_type()==DemandType.pathless)
                continue;

            Long origin_node_id = demand_profile.get_origin_node_id();
            Long destination_node_id = demand_profile.get_destination_node_id();
            Long commodity_id = demand_profile.commodity.getId();

            ODPair odpair = new ODPair(origin_node_id,destination_node_id,commodity_id);
            ODInfo odinfo;
            if(odmap.containsKey(odpair)){
                odinfo = odmap.get(odpair);
            } else {
                odinfo = new ODInfo(odpair, myapi.scn);
                odmap.put(odpair,odinfo);
            }
            odinfo.add_demand_profile(demand_profile);
        }

        return new ArrayList(odmap.values());
    }

    /**
     * Integrate the demands to obtain the total number of trips that will take place.
     * @return The number of trips.
     */
    public double get_total_trips() {
        return myapi.scn.data_demands.values().stream()
                .map(x->x.get_total_trips())
                .reduce(0.0,Double::sum);
    }

    ////////////////////////////////////////////////////////
    // sensors
    ////////////////////////////////////////////////////////

    /**
     * Get the total number of sensors in the scenario.
     * @return Number of sensors
     */
    public int get_num_sensors(){
        return myapi.scn.sensors.size();
    }

    /**
     * Get information for all sensors in the scenario.
     * @return A set of SensorInfo
     * @see SensorInfo
     */
    public Set<SensorInfo> get_sensors(){
        Set<SensorInfo> x = new HashSet<>();
        for(AbstractSensor y : myapi.scn.sensors.values())
            x.add(new SensorInfo(y));
        return x;
    }

    /**
     * Get information for a specific sensor.
     * @param id Id of the requested sensor
     * @return A SensorInfo object
     * @see SensorInfo
     */
    public SensorInfo get_sensor_with_id(long id){
        AbstractSensor sensor = myapi.scn.sensors.get(id);
        return sensor==null ? null : new SensorInfo(sensor);
    }

    ////////////////////////////////////////////////////////
    // controllers
    ////////////////////////////////////////////////////////

    /**
     * Get the total number of controllers in the scenario.
     * @return an integer.
     */
    public int get_num_controllers(){
        return myapi.scn.controllers.size();
    }

    /**
     * Get information for all controllers in the scenario.
     * @return a set of ControllerInfo
     * @see ControllerInfo
     */
    public Set<ControllerInfo> get_controllers(){
        Set<ControllerInfo> x = new HashSet<>();
        for(AbstractController y : myapi.scn.controllers.values())
            x.add(new ControllerInfo(y));
        return x;
    }

    /**
     * Get information for a specific myController.
     * @param id : [long] integer id of the myController.
     * @return A ControllerInfo object
     * @see ControllerInfo
     */
    public ControllerInfo get_controller_with_id(long id){
        AbstractController controller = myapi.scn.controllers.get(id);
        return controller==null ? null : new ControllerInfo(controller);
    }

//    /**
//     *
//     * @param id Undocumented
//     * @return Undocumented
//     */
//    public AbstractController get_actual_controller_with_id(long id){
//        return scenario.controllers.get(id);
//    }

    ////////////////////////////////////////////////////////
    // actuators
    ////////////////////////////////////////////////////////

    /**
     * Get the total number of actuators in the scenario
     * @return an integer.
     */
    public int get_num_actuators(){
        return myapi.scn.actuators.size();
    }

    /**
     * Get information for all actuators in the scenario.
     * @return a set of ActuatorInfo
     * @see ActuatorInfo
     */
    public Set<ActuatorInfo> get_actuators(){
        Set<ActuatorInfo> x = new HashSet<>();
        for(AbstractActuator y : myapi.scn.actuators.values())
            x.add(create_actuator_info(y));
        return x;
    }

    /**
     * Get information for a specific actuator.
     * @param id : [long] integer id of the actuator.
     * @return A ActuatorInfo object
     * @see ActuatorInfo
     */
    public ActuatorInfo get_actuator_with_id(long id){
        return create_actuator_info( myapi.scn.actuators.get(id) );
    }

    ////////////////////////////////////////////////////////
    // animation info
    ////////////////////////////////////////////////////////

//    /**
//     *
//     * @param link_ids Undocumented
//     * @return Undocumented
//     * @throws OTMException Undocumented
//     */
//    public AnimationInfo get_animation_info(List<Long> link_ids) throws OTMException {
//        return new AnimationInfo(scenario,link_ids);
//    }
//
//    /**
//     * Undocumented
//     * @return Undocumented
//     * @throws OTMException Undocumented
//     */
//    public AnimationInfo get_animation_info() throws OTMException {
//        return new AnimationInfo(scenario);
//    }

    ////////////////////////////////////////////////////////
    // private
    ////////////////////////////////////////////////////////

    private ActuatorInfo create_actuator_info(AbstractActuator actuator){

        if(actuator==null)
            return null;

        if(actuator instanceof ActuatorSignal)
            return new SignalInfo((ActuatorSignal) actuator);

        return new ActuatorInfo(actuator);
    }
}
