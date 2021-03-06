package output;

import output.events.EventWrapperTravelTime;
import commodity.Commodity;
import dispatch.Dispatcher;
import error.OTMException;
import models.vehicle.spatialq.MesoVehicle;
import models.vehicle.spatialq.Queue;
import runner.RunParameters;
import common.Scenario;

public class OutputTravelTime extends AbstractOutputEvent implements InterfaceVehicleListener  {

    //////////////////////////////////////////////////////
    // construction
    //////////////////////////////////////////////////////

    public OutputTravelTime(Scenario scenario, String prefix, String output_folder) {
        super(scenario, prefix, output_folder);
        this.type = Type.vehicle_travel_time;
    }

    //////////////////////////////////////////////////////
    // InterfaceOutput
    //////////////////////////////////////////////////////

    @Override
    public String get_output_file() {
        return write_to_file ? super.get_output_file() + "_vehicle_travel_time.txt" : null;
    }

    @Override
    public void register(RunParameters props, Dispatcher dispatcher) throws OTMException {
        // register with all commodities
        for (Commodity c : scenario.commodities.values())
            c.add_vehicle_event_listener(this);
    }

    //////////////////////////////////////////////////////
    // InterfaceVehicleListener
    //////////////////////////////////////////////////////

    @Override
    public void move_from_to_queue(float timestamp, MesoVehicle vehicle, Queue from_queue, Queue to_queue) throws OTMException {
        write(new EventWrapperTravelTime(timestamp,vehicle.getId(),from_queue,to_queue));
    }

    //////////////////////////////////////////////////////
    // InterfacePlottable
    //////////////////////////////////////////////////////

    @Override
    public String get_yaxis_label() {
        return "travel time [sec]";
    }
}
