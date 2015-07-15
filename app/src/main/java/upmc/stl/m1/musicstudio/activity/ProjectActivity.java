package upmc.stl.m1.musicstudio.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Timer;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import upmc.stl.m1.musicstudio.R;
import upmc.stl.m1.musicstudio.fragment.MixFragment;
import upmc.stl.m1.musicstudio.fragment.TrackFragment;
import upmc.stl.m1.musicstudio.tools.Block;
import upmc.stl.m1.musicstudio.tools.PlayBarTimer;
import upmc.stl.m1.musicstudio.view.DrawSecondsView;

public class ProjectActivity extends ActionBarActivity
implements TrackFragment.OnFragmentInteraction, MixFragment.OnFragmentInteraction {

    public enum PlayerStatus { PLAYING, PAUSED, STOPPED };

    private Menu menu;
    private int lastId;             // dernier id pour gérer le compteur en cas de suppression de piste
    private int tracksCount;        // compteur de pistes
    private String projectName;     // nom du projet courant
    private String projectPath;     // chemin du projet courant
    private boolean projectSaved;   // projet sauvegardé
    private boolean tick;
    private ArrayList<TrackFragment> tracks;
    private MixFragment mixer;
    private Timer clock;
    private PlayerStatus playerStatus;
    private Block copiedBlock;
    private int selected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.prepareLaunch();
        setContentView(R.layout.activity_main);
        this.mixer = new MixFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.mix_layout, this.mixer).commit();
        this.tracks = new ArrayList<TrackFragment>();
        this.clock = new Timer();
        this.playerStatus = PlayerStatus.STOPPED;
        this.addTrack(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        addSeconds();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch(id) {
            case R.id.action_tick:

                return true;
            case R.id.action_play_all:
                this.manageGlobalPlay(1);
                return true;
            case R.id.action_pause_all:
                this.manageGlobalPlay(2);
                return true;
            case R.id.action_stop_all:
                this.manageGlobalPlay(3);
                return true;
            case R.id.action_mix:
                this.mixTracks();
                return true;
            case R.id.action_new_project:
                this.createNewProject(true);
                return true;
            case R.id.action_open_project:
                selectProject();
                return true;
            case R.id.action_save_project:
                if (this.projectSaved) updateXML();
                else enterProjectName();
                return true;
            case R.id.action_save_as_project:
                enterProjectName();
                return true;
            //case R.id.action_export_project:
              //  return true;
            case R.id.action_quit_app:
                this.finish();
                System.exit(0);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        // suppression des fichiers audio non enregistrés
        File parentDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC + "/MusicStudio").getPath());
        File[] files = parentDir.listFiles();
        for (File f : files) {
            if (!f.isDirectory()) f.delete();
        }
        super.finish();
    }

    /**
     * Fonction d'ajout des secondes à la barre de temps
     */
    public void addSeconds() {
        RelativeLayout timeBar = (RelativeLayout) findViewById(R.id.tracks_time);
        timeBar.addView(new DrawSecondsView(this, timeBar.getHeight(), timeBar.getWidth()));
    }

    /**
     * Initialise un Fragment "TrackFragment" vide et l'ajoute à l'activité.
     * Une piste vide est alors ajoutée à la vue du projet en cours.
     * @param view
     */
    public void addTrack(View view) {

        // on récupère l'index du temps
        float index = 0;
        if (this.tracksCount > 0) index = this.tracks.get(0).getTimeIndex();

        // on prépare l'id à passer au fragment
        this.lastId++;
        Bundle bundle = new Bundle();
        bundle.putInt("idTrack", this.lastId);
        bundle.putFloat("timeIndex", index);

        // on instancie le fragment
        TrackFragment trackFragment = new TrackFragment();
        trackFragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction().add(R.id.tracks, trackFragment).commit();

        // on ajoute le framgnet à la liste des piste du projet
        this.tracks.add(trackFragment);
        this.tracksCount++;
    }

    /**
     * Charger une piste depuis un projet sauvegardé
     */
    public void loadTrack(String trackXML) {

        Bundle bundle = new Bundle();
        bundle.putInt("idTrack", this.lastId++);
        bundle.putFloat("timeIndex", 0);
        bundle.putString("xml", trackXML);

        // on instancie le fragment
        TrackFragment trackFragment = new TrackFragment();
        trackFragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction().add(R.id.tracks, trackFragment).commit();

        // on ajoute le framgnet à la liste des piste du projet
        this.tracks.add(trackFragment);
    }

    /**********************************************************************************************
     *
     * FONCTIONS DE LA BARRE DE MENU
     *
     **********************************************************************************************/

    /**
     * Fonction de gestion du métronome
     */
    public void manageTick() {
        MenuItem item = this.menu.findItem(R.id.action_tick);
        if (!this.tick) {
            item.setIcon(R.drawable.ic_action_time_ok);
            this.tick = true;
        } else {
            item.setIcon(R.drawable.ic_action_time_ko);
            this.tick = false;
        }
    }

    /**
     * Création d'un nouveau projet
     */
    public void createNewProject(boolean addTrack) {
        this.lastId = 0;
        this.tracksCount = 0;
        this.projectName = "";
        this.projectPath = "";
        this.projectSaved = false;
        this.clock.cancel();
        this.clock.purge();
        this.tracks.clear();
        this.clock = new Timer();
        this.playerStatus = PlayerStatus.STOPPED;
        getSupportFragmentManager().beginTransaction().remove(this.mixer).commit();
        this.mixer = new MixFragment();
        getSupportFragmentManager().beginTransaction().add(R.id.mix_layout, this.mixer).commit();

        // supprimer les infos du projet précédent si il existe
        LinearLayout tracks = (LinearLayout) this.findViewById(R.id.tracks);
        if(tracks.getChildCount() > 0) tracks.removeAllViews();

        // préparer le nouveau projet
        if (addTrack) this.addTrack(this.findViewById(R.id.tracks));

        TextView pName = (TextView) findViewById(R.id.project_name_text);
        pName.setText("New Project");
    }

    /**
     * Fonction de gestion de la lecture globale
     */
    public void manageGlobalPlay(int choice) {
        switch (this.playerStatus) {
            case PLAYING:
                if (choice == 2) { // pause
                    this.clock.cancel();
                    this.clock = new Timer();
                    for (TrackFragment f : this.tracks) f.pausePlaying();
                    this.playerStatus = PlayerStatus.PAUSED;
                }
                else if (choice == 3) { // stop
                    this.clock.cancel();
                    this.clock.purge();
                    this.clock = new Timer();
                    for (TrackFragment f : this.tracks) f.stopPlaying();
                    this.playerStatus = PlayerStatus.STOPPED;
                }
                break;
            case PAUSED:
                if (choice == 1) { // play
                    for (TrackFragment f : this.tracks) {
                        f.resumePlaying();
                    }
                    PlayBarTimer task = new PlayBarTimer(this.tracks);
                    this.clock.scheduleAtFixedRate(task, 0, 12); // 12 = arrondi supérieur donc approximatif...
                    this.playerStatus = PlayerStatus.PLAYING;
                }
                else if (choice == 3) { // stop
                    this.clock.cancel();
                    this.clock.purge();
                    this.clock = new Timer();
                    for (TrackFragment f : this.tracks) f.stopPlaying();
                    this.playerStatus = PlayerStatus.STOPPED;
                }
                break;
            case STOPPED:
                if (choice == 1) {
                    PlayBarTimer task = new PlayBarTimer(this.tracks);
                    this.clock.scheduleAtFixedRate(task, 0, 12); // 12 = arrondi supérieur donc approximatif...
                    this.playerStatus = PlayerStatus.PLAYING;
                }
                break;
            default:
                break;
        }
    }

    /**
     * Fonction de mixage des pistes
     */
    public void mixTracks() {
        LinearLayout mixLayout = (LinearLayout) findViewById(R.id.mix_layout);
        if (mixLayout.getVisibility() == View.INVISIBLE) {
            this.mixer.updateConsole();
            mixLayout.setVisibility(View.VISIBLE);
            mixLayout.bringToFront();
        } else {
            mixLayout.setVisibility(View.INVISIBLE);
        }
    }

    /**********************************************************************************************
     *
     * FONCTIONS DE SAUVEGARDE / OUVERTURE D'UN PROJET
     *
     **********************************************************************************************/

    /**
     * Fonction de sauvegarde du projet courant
     */
    public void updateXML() {
        // mise à jour des pistes
        for (TrackFragment f : this.tracks) {
            f.setPath(this.projectPath);
            f.transferFiles();
            f.deleteUnusedFiles(this.projectPath);
        }
        // remplissage du fichier XML
        File file = new File(this.projectPath + "/project.xml");
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(file));

            StringBuilder content = new StringBuilder();
            content.append("<project>\n");
            for (TrackFragment f : this.tracks) content.append(f.udpateXMLTrack());
            content.append("<lastId>" + this.lastId + "</lastId>\n");
            content.append("<tracksCount>" + this.tracksCount + "</tracksCount>\n");
            content.append("<projectPath>" + this.projectPath + "</projectPath>\n");
            content.append("</project>\n");
            out.write(content.toString().getBytes());
        }
        catch (FileNotFoundException e) { e.printStackTrace(); }
        catch (IOException e) { e.printStackTrace(); }
        finally {
            try { if (out != null)  out.close(); }
            catch (IOException e) { e.printStackTrace(); }
        }
    }

    /**
     * Créer un nouveau fichier XML de sauvegarde du projet
     */
    public void prepareProjectDirectory() {
        // création du dossier du projet
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC + "/MusicStudio//" + projectName);
        path.mkdirs();
        // création du fichier XML de stockage des informations sur le projet
        try {
            BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(this.projectPath + "/project.xml"));
            os.close();
        }
        catch (FileNotFoundException e) { e.printStackTrace(); }
        catch (IOException e) { e.printStackTrace(); }
        this.projectSaved = true;
        this.updateXML();
    }

    /**
     * Prompt pour demander le nom du projet
     */
    public void enterProjectName() {

        // préparation du layout
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.prompt_project_name, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(promptsView);
        builder.setCancelable(false);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        // listener sur le nom choisi
        final EditText userInput = (EditText) promptsView.findViewById(R.id.project_name_edit);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                projectName = userInput.getText().toString();
                boolean available = true;
                File parentDir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_MUSIC + "/MusicStudio").getPath());
                File[] files = parentDir.listFiles();
                for (File f : files) {
                    if (f.isDirectory()) {
                        if (f.getName().equals(projectName)) {
                            Toast toast = Toast.makeText(getApplicationContext(), "This name already exists!", Toast.LENGTH_SHORT);
                            toast.show();
                            available = false;
                            break;
                        }
                    }
                }

                if (available) {
                    TextView pName = (TextView) findViewById(R.id.project_name_text);
                    pName.setText(userInput.getText());
                    projectPath = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_MUSIC + "/MusicStudio//" + projectName).getPath();
                    prepareProjectDirectory();
                } else projectName = "";
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Selection d'un projet existant à ouvrir
     */
    public void selectProject() {

        this.selected = 0;
        int count = 0;
        int index = 0;

        // récupération des projets enregistrés
        File parentDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC + "/MusicStudio").getPath());
        File[] files = parentDir.listFiles();
        for (File f : files) if (f.isDirectory()) count++;
        final CharSequence[] choices = new CharSequence[count];
        for (File f : files) if (f.isDirectory()) { choices[index] = f.getName(); index++; }

        // création de la fenêtre de choix
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Select project to open :");
        builder.setSingleChoiceItems(choices, selected, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selected = which;
            }
        });
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                projectPath = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_MUSIC + "/MusicStudio//" + choices[selected]).getPath();
                openProject(choices[selected].toString(), Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_MUSIC + "/MusicStudio//" + choices[selected] + "/project.xml").getPath());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog choiceDialog = builder.create();
        choiceDialog.show();
    }

    /**
     * Ouverture d'un projet existant sélectionné
     */
    public void openProject(String name, String xml) {

        // préparation du nouveau projet
        createNewProject(false);
        ((TextView) findViewById(R.id.project_name_text)).setText(name);

        // parser le DOM du fichier XML
        Document dom = null;
        try { dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new FileInputStream(xml)); }
        catch (ParserConfigurationException e) { e.printStackTrace(); }
        catch (SAXException e) { e.printStackTrace(); }
        catch (IOException e) { e.printStackTrace(); }

        // on récupère les éléments
        if (dom != null) {
            NodeList nodes_project = dom.getDocumentElement().getChildNodes();

            // on parcourt le projet
            for (int i=0 ; i<nodes_project.getLength() ; i++) {
                Node node_project = nodes_project.item(i);
                if (node_project instanceof Element) {
                    Element child_project = (Element) node_project;

                    // on parcourt les pistes et on prépare une chaine xml à passer à la piste
                    if (child_project.getNodeName().equals("track")) {
                        StringBuilder builder = new StringBuilder();
                        builder.append("<track>\n");
                        NodeList nodes_track = node_project.getChildNodes();
                        for (int j=0 ; j<nodes_track.getLength() ; j++) {
                            Node node_track = nodes_track.item(j);
                            if (node_track instanceof Element) {
                                Element child_track = (Element) node_track;
                                switch (child_track.getNodeName()) {
                                    case "name":
                                        builder.append("<name>" + nodes_track.item(j).
                                                getFirstChild().getTextContent() + "</name>\n");
                                        break;
                                    case "idTrack":
                                        builder.append("<idTrack>" + nodes_track.item(j).
                                                getFirstChild().getTextContent() + "</idTrack>\n");
                                        break;
                                    case "idSample":
                                        builder.append("<idSample>" + nodes_track.item(j).
                                                getFirstChild().getTextContent() +"</idSample>\n");
                                        break;
                                    case "path":
                                        builder.append("<path>" + nodes_track.item(j).
                                                getFirstChild().getTextContent() + "</path>\n");
                                        break;
                                    case "block":

                                        // on parcourt les blocs
                                        builder.append("<block>\n");
                                        NodeList nodes_block = node_track.getChildNodes();
                                        for (int k=0 ; k<nodes_block.getLength() ; k++) {
                                            Node node_block = nodes_block.item(k);
                                            if (node_block instanceof Element) {
                                                Element child_block = (Element) node_block;
                                                switch (child_block.getNodeName()) {
                                                    case "id":
                                                        builder.append("<id>" + nodes_block.item(k).
                                                                getFirstChild().
                                                                getTextContent() + "</id>\n");
                                                        break;
                                                    case "name":
                                                        builder.append("<name>" + nodes_block.item(k).
                                                                getFirstChild().
                                                                getTextContent() + "</name>\n");
                                                        break;
                                                    case "start":
                                                        builder.append("<start>" + nodes_block.item(k).
                                                                getFirstChild().
                                                                getTextContent() + "</start>\n");
                                                        break;
                                                    case "length":
                                                        builder.append("<length>" + nodes_block.item(k).
                                                                getFirstChild().
                                                                getTextContent() + "</length>\n");
                                                        break;
                                                    case "path":
                                                        builder.append("<path>" + nodes_block.item(k).
                                                                getFirstChild().
                                                                getTextContent() + "</path>\n");
                                                        break;
                                                    default:
                                                        break;
                                                }
                                            }
                                        }
                                        builder.append("</block>\n");
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }
                        builder.append("</track>");
                        this.loadTrack(builder.toString());
                    }

                }
            }
            // on renseigne les informations de l'activité principale
            for (int i=0 ; i<nodes_project.getLength() ; i++) {
                Node node = nodes_project.item(i);
                if (node instanceof Element) {
                    Element child = (Element) node;
                    switch (child.getNodeName()) {
                        case "lastId":
                            this.lastId = Integer.parseInt(nodes_project.item(i).getFirstChild().getTextContent());
                            break;
                        case "tracksCount":
                            this.tracksCount = Integer.parseInt(nodes_project.item(i).getFirstChild().getTextContent());
                            break;
                        case "projectPath":
                            this.projectPath = nodes_project.item(i).getFirstChild().getTextContent();
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        this.projectSaved = true;
    }

    /**********************************************************************************************
     *
     * FONCTIONS DES INTERFACES DE COMMUNICATION
     *
     **********************************************************************************************/

    // LE FRAGMENT TRACK

    /**
     * Permet de récupérer le bloc copié par un fragment
     * @param block
     */
    public void onBlockCopy(Block block) {
        this.copiedBlock = block;
    }

    /**
     * Permet de recevoir la demande de copie d'un bloc
     */
    public void onBlockPaste(int idTrack) {
        if (this.copiedBlock != null) {
            for (TrackFragment f : this.tracks) {
                if (f.getIdTrack() == idTrack) {
                    f.copyBlock(this.copiedBlock);
                    break;
                }
            }
        }
    }

    /**
     * Suppression d'une piste
     */
    public void deleteTrack(int idTrack) {
        for (TrackFragment f : this.tracks) {
            if (f.getIdTrack() == idTrack) {
                this.tracks.remove(f);
                getSupportFragmentManager().beginTransaction().remove(f).commit();
                break;
            }
        }
        this.tracksCount--;
    }

    // LE FRAGMENT MIX

    /**
     * Permet de transmettre les pistes à un fragment de mixage pour l'affichage
     */
    public void onTracksRequired() {
        this.mixer.setTracks(this.tracks);
    }

    /**********************************************************************************************
     *
     * FONCTIONS DE GESTION DE L'ESPACE DE STOKAGE
     *
     **********************************************************************************************/

    /**
     *  Vérifie si le stockage externe est disponible pour l'écriture et la lecture
     */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) return true;
        return false;
    }

    /**
     * Vérifie si l'application peut être lancée.
     * Crée l'espace de stockage s'il n'exise pas déjà.
     */
    public boolean prepareLaunch() {
        if(isExternalStorageWritable()) {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC + "/MusicStudio");
            path.mkdirs();
        } else {
            Toast toast = Toast.makeText(getApplicationContext(), "External Memory Unavailable!", Toast.LENGTH_SHORT);
            toast.show();
            this.finish();
            System.exit(0);
        }
        return true;
    }

}
