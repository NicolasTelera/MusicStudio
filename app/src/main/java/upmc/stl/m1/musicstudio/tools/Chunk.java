package upmc.stl.m1.musicstudio.tools;

/**
 * Classe de stockage d'un chunk de données pour la manipulation des MPEG4-v2
 * Created by nicolas on 26/03/2015.
 */
public class Chunk {

    private int num;
    private int sampleDescIndex; // inutile ?
    private int samplesPerChunk;
    private int offset;

    public Chunk() {}

    public int getNum() {
        return num;
    }
    public int getSampleDescIndex() {
        return sampleDescIndex;
    }
    public int getSamplesPerChunk() {
        return samplesPerChunk;
    }
    public int getOffset() {
        return offset;
    }

    public void setNum(int num) {
        this.num = num;
    }
    public void setSampleDescIndex(int sampleDescIndex) {
        this.sampleDescIndex = sampleDescIndex;
    }
    public void setSamplesPerChunk(int samplesPerChunk) {
        this.samplesPerChunk = samplesPerChunk;
    }
    public void setOffset(int offset) {
        this.offset = offset;
    }

    @Override
    public String toString() {
        return "Chunk n°" + this.num + " d'index " + this.sampleDescIndex + ", contient " +
                this.samplesPerChunk + " samples, en position " + this.offset;
    }
}
