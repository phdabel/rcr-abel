package message;

public enum ColeagueType {
	
	FIREMEN("Corpo de Bombeiros"),
	POLICEOFFICE("Policiais"),
	AMBULANCETEAM("Equipe de Resgate");
	
	String name;
	
	private ColeagueType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

}
