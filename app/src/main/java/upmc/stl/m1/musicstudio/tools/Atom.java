package upmc.stl.m1.musicstudio.tools;

/**
 * Classe de stockage d'un atome provenant d'un fichier MPEG4-v2
 * Created by nicolas on 25/03/2015.
 */
public class Atom {

    private String name;
    private int index;
    private int size;
    private byte[] data;

    public Atom(int index) {
        this.index = index;
    }

    public String getName() {
        return this.name;
    }
    public int getSize() {
        return size;
    }
    public byte[] getData() {
        return data;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setSize(int size) {
        this.size = size;
        this.data = new byte[size];
    }
    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Atome '" + this.name + "' à l'index " + this.index + " de taille " + this.size;
    }

}
