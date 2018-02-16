package utils;

public enum TypeMessage {
	PUTCHUNK("PUTCHUNK"),
	STORED("STORED"),
	GETCHUNK("GETCHUNK"),
	CHUNK("CHUNK"),
	REMOVED("REMOVED"),
	DELETE("DELETE"); 

    private final String text;

    private TypeMessage(final String text) {
        this.text = text;
    }
    
    @Override
    public String toString() {
        return text;
    }
}