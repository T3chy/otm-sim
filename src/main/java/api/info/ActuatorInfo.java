package api.info;

import actuator.AbstractActuator;

public class ActuatorInfo {

    /** Integer id of the actuator. */
    public long id;

    /** Type of the actuator. */
    public AbstractActuator.Type type;

    public ActuatorInfo(AbstractActuator x){
        this.id = x.id;
//        this.type = x.getActuatorType();
    }

    public long getId() {
        return id;
    }

    public AbstractActuator.Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "ActuatorInfo{" +
                "id=" + id +
//                ", type=" + type +
                '}';
    }

}
