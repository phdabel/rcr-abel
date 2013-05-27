package message;

public enum Channel {

    FIRE_BRIGADE("Fire Brigade"),
    BROADCAST("Broadcast"),
    AMBULANCE("Ambulance"),
    POLICE_FORCE("Police Force");
    private String name;

    private Channel(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}

