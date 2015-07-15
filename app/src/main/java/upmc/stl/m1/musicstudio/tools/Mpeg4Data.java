package upmc.stl.m1.musicstudio.tools;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Classe de stockage des données relatives à un fichier MPEG4-v2.
 * Created by nicolas on 25/03/2015.
 */
public class Mpeg4Data {

    private ArrayList<Atom> atoms;
    private ArrayList<Chunk> chunks;
    private ArrayList<Integer> byteCountBySample; // stockage des taille des samples en bytes (samples dans l'ordre)
    private byte[] totalData;                     // (ArrayList car on doit la remplir avant de savoir sa taille)

    public Mpeg4Data(String path) {
        fillTotalData(path);
        this.atoms = new ArrayList<Atom>();
        this.chunks = new ArrayList<Chunk>();
        this.byteCountBySample = new ArrayList<Integer>();
        this.parseMPEG4File();
    }

    public void fillTotalData(String path) {

        // Récupération des données dans un tableau de bytes
        try {
            File file = new File(path);
            DataInputStream dis = new DataInputStream(new FileInputStream(file));
            this.totalData = new byte[0];
            this.totalData = new byte[dis.available()];
            dis.readFully(this.totalData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Atom getAtomByName(String name) {
        for(Atom a : this.atoms)
            if(a.getName().equals(name)) return a;
        return null;
    }

    public void findMoovInfos() {

        byte[] data = this.getAtomByName("moov").getData();
        byte[] temp;
        StringBuilder builder;
        boolean done = false;
        int index = 8;
        int size;
        String name;

        int[] exec = new int[3]; // stocker les index des atomes pour ensuite les parser dans l'ordre voulu (stsc, stco, stsz)

        try {

            // parsing complet du fichier pour récupérer les 4 atomes principaux
            do {

                builder = new StringBuilder();
                Atom atom = new Atom(index);

                // récupération du nom de l'atome
                temp = new byte[4];
                for (int i=0 ; i<4 ; i++) temp[i] = data[index + i + 4];
                name = new String(temp, "UTF-8");

                // si on est sur l'atome voulu, on descend d'un niveau
                // sinon on continue le traitement
                if (name.equals("trak")) index += 8;
                else if (name.equals("mdia")) index += 8;
                else if (name.equals("minf")) index += 8;
                else if (name.equals("stbl")) index += 8;
                else {



                    // récupération de la longueur de l'atome
                    for (int i=0 ; i<4 ; i++) builder.append(String.format("%02x", data[index + i]));
                    size = Integer.parseInt(builder.toString(), 16);

                    // parsing des atomes recherchés
                    if (name.equals("stsc")) exec[0] = index;
                    else if (name.equals("stco")) exec[1] = index;
                    else if (name.equals("stsz")) exec[2] = index;

                    // vérification de la condition de sortie
                    if((index += size) >= data.length) done = true;

                }

            } while(!done);

            this.parseAtomByName("stsc", exec[0]);
            this.parseAtomByName("stco", exec[1]);
            this.parseAtomByName("stsz", exec[2]);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        int cpt = 0;
        for(Chunk c : this.chunks) {
            System.out.println(c);
            /*
            for (int i=0 ; i<c.getSamplesPerChunk() ; i++) {
                System.out.println("- sample n°" + cpt +  " : " + this.byteCountBySample.get(cpt) + " bytes");
                cpt++;
            }
            */
        }

    }

    public void parseAtomByName(String name, int index) {

        byte[] data = this.getAtomByName("moov").getData();
        StringBuilder builder = new StringBuilder();
        int size;
        int cpt = 0;

        // récupération de la longueur de l'atome
        for (int i=0 ; i<4 ; i++) builder.append(String.format("%02x", data[index + i]));
        size = Integer.parseInt(builder.toString(), 16);

        switch(name) {


            case "stsc" : /* -- récupération du nombre de samples par chunk -- */

                index += 16;
                builder = new StringBuilder();
                for (int i=0 ; i<size-16 ; i += 12) {
                    Chunk chunk = new Chunk();
                    StringBuilder numBuilder = new StringBuilder();
                    StringBuilder countBuilder = new StringBuilder();
                    StringBuilder descBuilder = new StringBuilder();
                    for (int j = 0; j < 4; j++) {
                        numBuilder.append(String.format("%02x", data[index + i + j]));
                        countBuilder.append(String.format("%02x", data[index + i + j + 4]));
                        descBuilder.append(String.format("%02x", data[index + i + j + 8]));
                    }
                    chunk.setNum(Integer.parseInt(numBuilder.toString(), 16));
                    chunk.setSamplesPerChunk(Integer.parseInt(countBuilder.toString(), 16));
                    chunk.setSampleDescIndex(Integer.parseInt(descBuilder.toString(), 16));
                    this.chunks.add(chunk);
                }

                break;

            case "stco" : /* -- récupération de l'offset (sur les données totales) des chunks - */

                index += 16;
                builder = new StringBuilder();

                for (int i=0 ; i<size-16 ; i += 4) {
                    builder = new StringBuilder();
                    for (int j = 0; j < 4; j++)
                        builder.append(String.format("%02x", data[index + i + j]));
                    this.chunks.get(cpt).setOffset(Integer.parseInt(builder.toString(), 16));
                    cpt++;
                }

                break;

            case "stsz" : /* -- récupération de la taille de chaque sample - */

                index += 20;
                builder = new StringBuilder();

                for (int i=0 ; i<size-20 ; i += 4) {
                    builder = new StringBuilder();
                    for (int j = 0; j < 4; j++)
                        builder.append(String.format("%02x", data[index + i + j]));
                    this.byteCountBySample.add(Integer.parseInt(builder.toString(), 16));
                    //this.sizeRawData += Integer.parseInt(builder.toString(), 16);
                }

                break;

        }
    }

    public short[] computeDataToDraw() {

        int length = 0;
        for (Chunk c : this.chunks) length += c.getSamplesPerChunk();

        short[] values = new short[length];
        int indexTotal = 0;
        int indexSamples = 0;
        ByteBuffer wrapped = null;

        for (Chunk chunk : this.chunks) {
            indexTotal = chunk.getOffset();
            for (int i=0 ; i<chunk.getSamplesPerChunk() ; i++) {
                byte[] sample = new byte[this.byteCountBySample.get(indexSamples)];
                for(int j=0 ; j<this.byteCountBySample.get(indexSamples) ; j++) {
                    sample[j] = this.totalData[indexTotal + j];
                }
                wrapped = ByteBuffer.wrap(sample);
                values[indexSamples++] = wrapped.getShort();
                indexTotal += sample.length;
            }
        }

        return values;

    }

    public void parseMPEG4File() {

        try {

            // PARSING DU FICHIER MPEG-4
            byte[] temp;
            StringBuilder builder;
            int index = 0;
            boolean done = false;

            // parsing complet du fichier pour récupérer les 4 atomes principaux
            do {

                builder = new StringBuilder();
                Atom atom = new Atom(index);

                // récupération de la longueur de l'atome
                for (int i=0 ; i<4 ; i++) builder.append(String.format("%02x", this.totalData[index + i]));
                atom.setSize(Integer.parseInt(builder.toString(), 16));

                // récupération des données de l'atome
                temp = new byte[atom.getSize()];
                for (int i=0 ; i<atom.getSize() ; i++) temp[i] = this.totalData[index + i];
                atom.setData(temp);

                // récupération du nom de l'atome
                temp = new byte[4];
                for (int i=0 ; i<4 ; i++) temp[i] = atom.getData()[i + 4];
                atom.setName(new String(temp, "UTF-8"));

                this.atoms.add(atom);
                if((index += atom.getSize()) >= this.totalData.length) done = true;

            } while(!done);

            System.out.println("====================================================");
            System.out.println("LONGUEUR TOTALE DES DONNEES : " + this.totalData.length);
            for(Atom a : this.atoms) System.out.println(a);
            System.out.println("====================================================");

            // récupération du nombre d'entrées (chunks) dans le média et de son début
            this.findMoovInfos();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
