package sensor;

import dispatch.Dispatcher;
import dispatch.EventPoke;
import dispatch.Pokable;
import error.OTMErrorLog;
import common.InterfaceScenarioElement;
import common.Scenario;
import common.ScenarioElementType;
import error.OTMException;

public abstract class AbstractSensor implements Pokable, InterfaceScenarioElement {

    public enum Type {
        fixed
    }

    public Long id;
    public Type type;
    public float dt;
    public double dt_inv;

    public Object target;

    /////////////////////////////////////////////////////////////////////
    // construction
    /////////////////////////////////////////////////////////////////////


    public AbstractSensor(Long id, Type type, float dt) {
        this.id = id;
        this.type = type;
        this.dt = dt;
        this.dt_inv = 3600d/dt;
    }

    public AbstractSensor(jaxb.Sensor jaxb_sensor) {
        this(jaxb_sensor.getId(),Type.valueOf(jaxb_sensor.getType()),jaxb_sensor.getDt());
    }

    ////////////////////////////////////////////
    // InterfaceScenarioElement
    ///////////////////////////////////////////

    public void initialize(Scenario scenario) throws OTMException {
        Dispatcher dispatcher = scenario.dispatcher;
        dispatcher.register_event(new EventPoke(dispatcher,10,dispatcher.current_time,this));
    }

    @Override
    public final ScenarioElementType getSEType() {
        return ScenarioElementType.sensor;
    }

    @Override
    public final Long getId() {
        return id;
    }

    @Override
    public OTMErrorLog to_jaxb() {
        return null;
    }

    /////////////////////////////////////////////////////////////////////
    // update
    /////////////////////////////////////////////////////////////////////

    abstract public void take_measurement(Dispatcher dispatcher, float timestamp);

    @Override
    public void poke(Dispatcher dispatcher, float timestamp) {
        take_measurement(dispatcher,timestamp);

        // write to output
//        if(event_output!=null)
//            event_output.write(timestamp,new EventWrapperSensor(measurement));

        // wake up in dt, if dt is defined
        if(dt>0)
            dispatcher.register_event(new EventPoke(dispatcher,1,timestamp+dt,this));
    }

    /////////////////////////////////////////////////////////////////////
    // InterfaceEventWriter
    /////////////////////////////////////////////////////////////////////

//    @Override
//    public void set_event_output(AbstractOutputEvent e) throws OTMException {
//        if(event_output !=null)
//            throw new OTMException("multiple listeners for sensor");
//        if(!(e instanceof OutputSensor))
//            throw new OTMException("Wrong type of listener");
//        event_output = (OutputSensor) e;
//    }
}
