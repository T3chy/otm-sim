package control.rampmetering;

import actuator.AbstractActuator;
import actuator.ActuatorMeter;
import common.LaneGroupSet;
import control.command.CommandNumber;
import error.OTMException;
import jaxb.Controller;
import common.Link;
import jaxb.Roadparam;
import common.Scenario;
import sensor.FixedSensor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ControllerAlinea extends AbstractControllerRampMetering {

    public Map<Long,AlineaParams> params;  // parameters per actuator

    ///////////////////////////////////////////////////
    // construction
    ///////////////////////////////////////////////////

    public ControllerAlinea(Scenario scenario, Controller jaxb_controller) throws OTMException {
        super(scenario, jaxb_controller);
        params = new HashMap<>();
    }

    ///////////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////////////

    @Override
    public void initialize(Scenario scenario) throws OTMException {
        super.initialize(scenario);

        params = new HashMap<>();
        for(AbstractActuator abs_act : actuators.values()){
            ActuatorMeter act = (ActuatorMeter) abs_act;
            AlineaParams param = new AlineaParams();

            FixedSensor ml_sensor = (FixedSensor) sensors.iterator().next();
            Link ml_link = ml_sensor.get_link();

            Roadparam p = ml_link.road_param_full;
            param.gain_per_sec = p.getSpeed() * 1000f / 3600f / ml_link.length ; // [kph]*1000/3600/[m] -> [mps]
            float critical_density_vpkpl = p.getCapacity() / p.getSpeed();  // vpkpl
            param.ref_density_veh = critical_density_vpkpl * ml_link.full_lanes * ml_link.length / 1000f;

            param.ref_link = ml_link;

            // all lanegroups in the actuator must be in the same link
            LaneGroupSet lgs = (LaneGroupSet)act.target;
            Set<Link> ors = lgs.lgs.stream().map(lg->lg.link).collect(Collectors.toSet());

            if(ors.size()!=1)
                throw new OTMException("All lanegroups in any single actuator used by an Alinea controller must belong to the same link.");

            params.put(abs_act.id , param);
            command.put(act.id,
                    new CommandNumber(Float.isInfinite(act.max_rate_vps) ? (float) ml_link.full_lanes*900f/3600f : act.max_rate_vps)
            );
        }
    }

    @Override
    protected float compute_nooverride_rate_vps(ActuatorMeter act,float timestamp) {
        AlineaParams p = params.get(act.id);
        float density_veh = (float) p.ref_link.get_veh();
        float previous_rate_vps = ((CommandNumber) command.get(act.id)).value;
        return previous_rate_vps +  p.gain_per_sec * (p.ref_density_veh - density_veh);
    }

    public class AlineaParams {
        float gain_per_sec;
        float ref_density_veh;
        Link ref_link;
    }
}
