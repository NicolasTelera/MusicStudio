package upmc.stl.m1.musicstudio.tools;

/**
 * Created by nicolas on 07/02/2015.
 * Bloc permettant de stocker un enregistrement sur une piste.
 * @name : le nom du bloc
 * @start : la position de départ du bloc
 * @length : la durée de l'enregistrement
 */
public class Block {

    private int id;
    private String name;
    private float start;
    private float length;
    private String path;

    public Block(int id, String name, float start, float length, String path) {
        this.id = id;
        this.name = name;
        this.start = start;
        this.length = length;
        this.path = path;
    }

    public Block() {}

    public int getId() {
        return this.id;
    }

    public String getName() { return this.name; }

    public float getStart() {
        return this.start;
    }

    public float getLength() {
        return this.length;
    }

    public String getPath() {
        return this.path;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStart(float start) {
        this.start = start;
    }

    public void setLength(float length) {
        this.length = length;
    }

    public void setPath(String path) {
        this.path = path;
    }

}
