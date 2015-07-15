package upmc.stl.m1.musicstudio.fragment;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import upmc.stl.m1.musicstudio.task.TickTask;
import upmc.stl.m1.musicstudio.tools.Block;
import upmc.stl.m1.musicstudio.R;
import upmc.stl.m1.musicstudio.task.ConvertToMP4Task;
import upmc.stl.m1.musicstudio.task.RecordSoundTask;
import upmc.stl.m1.musicstudio.view.DrawSoundView;
import upmc.stl.m1.musicstudio.tools.Mpeg4Data;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnFragmentInteraction} interface
 * to handle interaction events.
 * Use the {@link TrackFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TrackFragment extends Fragment {

    // Interface de communication avec l'activité et son listener
    private OnFragmentInteraction interactionListener;
    public interface OnFragmentInteraction {
        public void onBlockCopy(Block block);
        public void onBlockPaste(int idTrack);
        public void deleteTrack(int idTrack);
    }

    // Variables locales
    private String name;
    private int idTrack;
    private int idSample;
    private String path;
    private long duration;
    private float timeIndex;
    private boolean moving;
    private RecordSoundTask recordTask;
    private ConvertToMP4Task convertTask;
    private RelativeLayout barLayout;
    private ArrayList<Block> blocks;
    private HashMap<Block, MediaPlayer> playTasks;
    private float[] balance;
    private int playingSample;
    private String xml;

    // Pour le tick
    private SoundPool soundPool;
    private boolean loaded;
    private float volume;
    private int soundID;

    public static TrackFragment newInstance() {
        TrackFragment fragment = new TrackFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public TrackFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle params = this.getArguments();
        this.idTrack = params.getInt("idTrack");
        this.name = "TRACK_" + this.idTrack;
        this.timeIndex = params.getFloat("timeIndex");
        if (params.containsKey("xml")) this.xml = params.getString("xml");
        this.idSample = 1;
        this.path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC +
                "/MusicStudio/track" + this.idTrack + "_sample").getPath();
        this.blocks = new ArrayList<Block>();
        this.playTasks = new HashMap<Block, MediaPlayer>();
        this.balance = new float[2];
        this.balance[0] = 1.0f;
        this.balance[1] = 1.0f;
        this.playingSample = -1;

        // chargement du tick
        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
        AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        this.volume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) /
                (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        this.soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        this.soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                loaded = true;
            }
        });
        this.soundID = this.soundPool.load(getActivity(), R.raw.tick, 1);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_track, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((TextView) getView().findViewById(R.id.track_name)).setText("TRACK_" + this.idTrack);

        // LISTENER sur la piste pour le menu popup
        RelativeLayout track = (RelativeLayout) getView().findViewById(R.id.track_sound_block);
        track.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                PopupMenu popupMenu = new PopupMenu(getActivity(), v);
                popupMenu.getMenuInflater().inflate(R.menu.menu_popup_track, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getTitle().toString()) {
                            case "Rename":
                                enterTrackName();
                                break;
                            case "Paste":
                                interactionListener.onBlockPaste(idTrack);
                                break;
                            case "Delete":
                                interactionListener.deleteTrack(idTrack);
                                break;
                            default:
                                break;
                        }
                        return true;
                    }
                });
                popupMenu.show();
                return true;
            }
        });

        // LISTENER sur le bouton d'enregistrement
        Button buttonRecord = (Button) getView().findViewById(R.id.track_record_button);
        buttonRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                record(v);
            }
        });

        // LISTENER sur le switch de mute
        Switch muteSwitch = (Switch) getView().findViewById(R.id.track_mute_switch);
        muteSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mute(isChecked);
            }
        });

        // récupération de la barre de temps
        barLayout = (RelativeLayout) getView().findViewById(R.id.play_time_bar);
        this.barLayout.setX(this.timeIndex);

        // Si le xml est renseigné, chargement des informations de la piste
        // on doit utiliser un listener pour attendre que la vue soit bien créée
        if (this.xml != null) {
            final RelativeLayout piste = (RelativeLayout) getView().findViewById(R.id.track_sound_block);
            piste.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    piste.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    if (xml != null) loadTrack();
                }
            });
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            interactionListener = (OnFragmentInteraction) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnBlockCopyListener, onBlockPaste and deleteTrack");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        interactionListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**********************************************************************************************
     *
     * ACCESSEURS ET MODIFICATEURS
     *
     **********************************************************************************************/

    public void setIdTrack(int id) {
        this.idTrack = id;
    }

    public void setIdSample(int id) {
        this.idSample = id;
    }

    public void setPath(String path) {
        this.path = path + "/track" + this.idTrack + "_sample";
    }

    public String getName() {
        return this.name;
    }

    public int getIdTrack() {
        return this.idTrack;
    }

    public float getTimeIndex() {
        return this.timeIndex;
    }

    public float[] getBalance() {
        return this.balance;
    }

    /**********************************************************************************************
     *
     * FONCTIONS DE MANIPULATION DE LA PISTE
     *
     **********************************************************************************************/

    /**
     * Prompt pour demander le nom du projet
     */
    public void enterTrackName() {

        // préparation du layout
        LayoutInflater li = LayoutInflater.from(getActivity());
        View promptsView = li.inflate(R.layout.prompt_track_name, null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(promptsView);
        builder.setCancelable(false);
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        // listener sur le nom choisi
        final EditText userInput = (EditText) promptsView.findViewById(R.id.track_name_edit);
        userInput.setText(((TextView) getView().findViewById(R.id.track_name)).getText());
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                name = userInput.getText().toString();
                ((TextView) getView().findViewById(R.id.track_name)).setText(name);
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Nécessaire pour attendre la fin de la tâche asynchrone de conversion avant
     * de dessiner le bloc sur la piste.
     */
    public interface ConvertCallback {
        public void onTaskDone();
    }

    /**
     * Enregistre un son.
     * @param view
     */
    public void record(View view) {

        Button recordButton = (Button) getView().findViewById(R.id.track_record_button);

        final String path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC + "/MusicStudio/track" + idTrack + "_sample" + idSample).getPath();

        if(this.recordTask == null || this.recordTask.getStatus() == AsyncTask.Status.FINISHED) {
            TickTask tickTask = new TickTask(this.soundPool, this.soundID, this.volume);
            tickTask.execute();
            // Lancement de l'enregistrement
            this.recordTask = new RecordSoundTask(this, path);
            this.recordTask.execute();
            recordButton.setText("STOP");
            // lancement de l'enregistrement
            this.duration = System.nanoTime();
        } else {
            this.duration = System.nanoTime() - this.duration;
            //System.out.println("duréé de la prise : " + (duration/1E9));
            // Fin de l'enregistrement
            this.recordTask.setIsRecording(false);
            recordButton.setText("RECORD");
            // Lancement de la tâche de conversion
            this.convertTask = new ConvertToMP4Task(this, path, new ConvertCallback() {
                @Override
                public void onTaskDone() {
                    // Préparation du block
                    Block block = new Block(idSample, "sample" + idSample, 0,
                            (int)duration, path);
                    addBlock(block);
                }
            });
            convertTask.execute();
        }

    }

    /**
     * Fonction de mute
     */
    public void mute(boolean isChecked) {
        if (isChecked) {
            for (MediaPlayer mp : this.playTasks.values()) {
                mp.setVolume(0.0f, 0.0f);
            }
        } else {
            for (MediaPlayer mp : this.playTasks.values()) {
                mp.setVolume(this.balance[0], this.balance[1]);
            }
        }
    }

    /**
     * Fonction de modification de la balance de la piste
     */
    public void changeBalance(float left, float right) {
        this.balance[0] = left;
        this.balance[1] = right;
        for (MediaPlayer mp : this.playTasks.values()) {
            mp.setVolume(left, right);
        }
    }

    /**
     * Ajout d'un bloc sur une piste
     * @param block
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN) // nécessaire car l'API à 18 n'est pas détectée...
    public void addBlock(final Block block) {

        // préparation d'un LinearLayout pour représenter le bloc
        final RelativeLayout layoutBlock = new RelativeLayout(getActivity());
        layoutBlock.setBackground(getResources().getDrawable(R.drawable.custom_border_block));

        // ajout d'un listener sur les mouvements du doigt pour déplacer le bloc        /!\ TRAVAILLER SUR LE DEPLACEMENT PLUS PRECIS
        layoutBlock.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                float widthOffset = getView().findViewById(R.id.track_buttons_block).getWidth();

                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    // on empêche le long click (et on supprime la vibration associée)
                    moving = true;
                    v.setHapticFeedbackEnabled(false);

                    // on gère le déplacement
                    float position = event.getRawX() - widthOffset;
                    if (position < 0) position = 0;
                    v.setX(position);
                    block.setStart(position);
                    //if (block.getId() == playingSample) ICI METTRE A JOUR LE LECTEUR ASSOCIE
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    // on rend le long click possible à nouveau (et la vibration associée)
                    moving = false;
                    v.setHapticFeedbackEnabled(true);
                }
                return false;
            }
        });

        // ajout d'un listener sur l'appui prolongé pour les options
        layoutBlock.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!moving) {
                    PopupMenu popupMenu = new PopupMenu(getActivity(), layoutBlock);
                    popupMenu.getMenuInflater().inflate(R.menu.menu_popup_block, popupMenu.getMenu());
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getTitle().toString()) {
                                case "Duplicate":
                                    duplicateBloc(block, layoutBlock);
                                    break;
                                case "Copy":
                                    interactionListener.onBlockCopy(block);
                                    break;
                                case "Delete":
                                    deleteBlock(block, layoutBlock);
                                    break;
                                default:
                                    break;
                            }
                            return true;
                        }
                    });
                    popupMenu.show();
                }
                return true;
            }
        });


        // ajout du block à la piste
        RelativeLayout piste = (RelativeLayout) getView().findViewById(R.id.track_sound_block);
        piste.addView(layoutBlock);

        // enregistrement du bloc dans la table de hachage
        MediaPlayer player = new MediaPlayer();
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                playingSample = -1;
            }
        });
        try {
            player.setDataSource(block.getPath() + ".mp4");
            player.prepare();
        } catch (IOException e) { e.printStackTrace(); }
        this.blocks.add(block);
        this.playTasks.put(block, player);

        // représentation du son dans le bloc généré
        this.drawSound(layoutBlock, block);
    }

    /**
     * Fonction de préparation pour la copie d'un bloc
     */
    public void copyBlock(Block block) {

        Block copy = new Block(this.idSample, "sample" + this.idSample,
                block.getStart(), block.getLength(), Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC + "/MusicStudio/track" + this.idTrack + "_sample" + this.idSample).getPath());

        // copie création du nouvea fichier audio
        try {
            byte[] buf = new byte[1024];
            int len;
            InputStream in = new FileInputStream(block.getPath() + ".raw");
            OutputStream out = new FileOutputStream(copy.getPath() + ".raw");
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in = new FileInputStream(block.getPath() + ".mp4");
            out = new FileOutputStream(copy.getPath() + ".mp4");
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close();
            out.close();
        }
        catch (FileNotFoundException e) { e.printStackTrace(); }
        catch (IOException e) { e.printStackTrace(); }

        this.addBlock(copy);
    }

    /**
     * Duplication d'un bloc
     */
    public void duplicateBloc(Block block, RelativeLayout layoutBlock) {

        // copie du fichier audio
        try {
            byte[] buf = new byte[1024];
            int len;
            InputStream in = new FileInputStream(block.getPath() + ".raw");
            OutputStream out = new FileOutputStream(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MUSIC + "/MusicStudio/track" + this.idTrack + "_sample" + this.idSample + ".raw").getPath());
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in = new FileInputStream(block.getPath() + ".mp4");
            out = new FileOutputStream(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MUSIC + "/MusicStudio/track" + this.idTrack + "_sample" + this.idSample + ".mp4").getPath());
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close();
            out.close();
        }
        catch (FileNotFoundException e) { e.printStackTrace(); }
        catch (IOException e) { e.printStackTrace(); }

        // copie du block
        Block newBlock = new Block();
        newBlock.setId(this.idSample);
        newBlock.setName("sample" + this.idSample);
        newBlock.setStart(block.getStart() + block.getLength() + 1);
        newBlock.setLength(block.getLength());
        newBlock.setPath(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC + "/MusicStudio/track" + this.idTrack + "_sample" + this.idSample).getPath());
        this.addBlock(newBlock);
    }

    /**
     * Suppression d'un bloc
     */
    public void deleteBlock(Block block, RelativeLayout layoutBlock) {
        // suppression du bloc enregistré
        this.blocks.remove(block);
        this.playTasks.remove(block);
        if (block.getId() == this.playingSample) this.playingSample = -1;
        // suppression du layout du bloc
        RelativeLayout track = (RelativeLayout) getView().findViewById(R.id.track_sound_block);
        track.removeView(layoutBlock);
    }

    /**********************************************************************************************
     *
     * FONCTIONS DE LECTURE
     *
     **********************************************************************************************/

    /**
     * Mettre en pause la lecture de la piste
     */
    public void pausePlaying() {
        for (MediaPlayer mp : this.playTasks.values()) {
            mp.pause();
        }
    }

    /**
     * Relancer la lecture arpès la pause
     */
    public void resumePlaying() {
        if (this.playingSample != -1) {
            for (Block b : this.playTasks.keySet()) {
                if (b.getId() == this.playingSample) {
                    this.playTasks.get(b).start();
                }
            }
        }
    }

    /**
     * Arrêter la lecure de la piste
     */
    public void stopPlaying() {
        if (this.playingSample != -1) {
            for (MediaPlayer mp : this.playTasks.values()) {
                mp.pause();
                mp.seekTo(0);
                // ne pas utiliser mp.stop(); qui empêche de lire derrière
            }
        }
        this.barLayout.setX(0);
        this.timeIndex = 0;

    }

    /**********************************************************************************************
     *
     * FONCTIONS DE SAUVEGARDE / OUVERTURE
     *
     **********************************************************************************************/

    /**
     * Fonction de copie des fichiers temporaires de son vers le dossier du projet
     */
    public void transferFiles() {
        try {
            String path = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MUSIC + "/MusicStudio/track" + this.idTrack + "_").getPath();
            InputStream in = null;
            OutputStream out = null;
            for (Block b : this.blocks) {
                if (b.getPath().equals(path + b.getName())) {
                    byte[] buf = new byte[1024];
                    int len;
                    in = new FileInputStream(b.getPath() + ".raw");
                    out = new FileOutputStream(this.path + b.getId() + ".raw");
                    while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                    in = new FileInputStream(b.getPath() + ".mp4");
                    out = new FileOutputStream(this.path + b.getId() + ".mp4");
                    while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                    b.setPath(this.path + b.getId());
                }
            }
            if (in != null)  in.close();
            if (out != null)  out.close();
        }
        catch (FileNotFoundException e) { e.printStackTrace(); }
        catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * Fonction de suppression des fichiers son plus utilisés à la sauvegarde
     */
    public void deleteUnusedFiles(String path) {

        boolean delete;
        File parentDir = new File(path);
        File[] files = parentDir.listFiles();
        for (File f : files) {
            if (!f.isDirectory()) {
                delete = true;
                if (!f.getName().equals("project.xml") && f.getName().contains("track" + this.idTrack)) {
                    for (Block b : this.blocks) {
                        if (f.getName().equals("track" + this.idTrack + "_" + b.getName() + ".raw") ||
                            f.getName().equals("track" + this.idTrack + "_" + b.getName() + ".mp4")) {
                            delete = false;
                            break;
                        }
                    }
                    if (delete) f.delete();
                }
            }
        }
    }

    /**
     * Remplir une partie du fichier XML de sauvegarde pour cette piste
     */
    public String udpateXMLTrack() {
        StringBuilder trackContent = new StringBuilder();
        trackContent.append("<track>\n");
        for (Block b : this.blocks) {
            trackContent.append("<block>\n");
            trackContent.append("<id>" + b.getId() + "</id>\n");
            trackContent.append("<name>" + b.getName() + "</name>\n");
            trackContent.append("<start>" + b.getStart() + "</start>\n");
            trackContent.append("<length>" + b.getLength() + "</length>\n");
            trackContent.append("<path>" + b.getPath() + "</path>\n");
            trackContent.append("</block>\n");
        }
        trackContent.append("<name>" + this.name + "</name>\n");
        trackContent.append("<idTrack>" + this.idTrack + "</idTrack>\n");
        trackContent.append("<idSample>" + this.idSample + "</idSample>\n");
        trackContent.append("<path>" + this.path + "</path>\n");
        trackContent.append("</track>\n");
        return trackContent.toString();
    }

    /**
     * Chargement des blocks sauvegardés dans un projet exitant
     */
    public void loadTrack() {

        // parser le DOM du fichier XML
        Document dom = null;
        try {
            dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new InputSource(new StringReader(this.xml)));
        }
        catch (ParserConfigurationException e) { e.printStackTrace(); }
        catch (SAXException e) { e.printStackTrace(); }
        catch (IOException e) { e.printStackTrace(); }

        // on récupère les éléments
        if (dom != null) {
            NodeList nodes_track = dom.getDocumentElement().getChildNodes();
            // on parcourt la piste
            for (int j=0 ; j<nodes_track.getLength() ; j++) {
                Node node_track = nodes_track.item(j);
                if (node_track instanceof Element) {
                    Element child_track = (Element) node_track;

                    // on met à jour les informations de pitse
                    switch (child_track.getNodeName()) {
                        case "name":
                            this.name = nodes_track.item(j).getFirstChild().getTextContent();
                            ((TextView) getView().findViewById(R.id.track_name)).setText(this.name);
                            break;
                        case "idTrack":
                            this.idTrack = Integer.parseInt(nodes_track.item(j).
                                    getFirstChild().
                                    getTextContent());
                            break;
                        case "idSample":
                            this.idSample = Integer.parseInt(nodes_track.item(j).
                                    getFirstChild().
                                    getTextContent());
                            break;
                        case "path":
                            this.path = nodes_track.item(j).getFirstChild().getTextContent();
                            break;
                        case "block":

                            // on ajoute le bloc
                            Block block = new Block();
                            NodeList nodes_block = node_track.getChildNodes();
                            for (int k=0 ; k<nodes_block.getLength() ; k++) {
                                Node node_block = nodes_block.item(k);
                                if (node_block instanceof Element) {
                                    Element child_block = (Element) node_block;
                                    switch (child_block.getNodeName()) {
                                        case "id":
                                            block.setId(Integer.parseInt(nodes_block.item(k).
                                                    getFirstChild().
                                                    getTextContent()));
                                            break;
                                        case "name":
                                            block.setName(nodes_block.item(k).getFirstChild().
                                                    getTextContent());
                                            break;
                                        case "start":
                                            block.setStart(Float.parseFloat(nodes_block.item(k).
                                                    getFirstChild().
                                                    getTextContent()));
                                            break;
                                        case "length":
                                            block.setLength(Float.parseFloat(nodes_block.item(k).
                                                    getFirstChild().
                                                    getTextContent()));
                                            break;
                                        case "path":
                                            block.setPath(nodes_block.item(k).getFirstChild().
                                                    getTextContent());
                                            break;
                                        default:
                                            break;
                                    }
                                }
                            }
                            this.addBlock(block);
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    /**********************************************************************************************
     *
     * FONCTIONS DE VISUALISATION
     *
     **********************************************************************************************/

    /**
     * Dessin de la forme d'un son
     * @param layoutBloc
     */
    public void drawSound(RelativeLayout layoutBloc, Block block) {

        // récupération du bloc à dessiner
        RelativeLayout piste = (RelativeLayout) getView().findViewById(R.id.track_sound_block);

        // traitement du fichier MPEG-4
        Mpeg4Data mpeg4Data = new Mpeg4Data(block.getPath() + ".mp4");
        short[] data = mpeg4Data.computeDataToDraw();
        block.setLength((data.length - 5) * 2);

        // lancement du dessin de la forme d'onde
        Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        DrawSoundView view = new DrawSoundView(getActivity(), piste.getHeight(), data);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((data.length-5)*2, piste.getHeight()-6);
        layoutBloc.setLayoutParams(params);
        layoutBloc.addView(view);
        layoutBloc.setX(block.getStart());
        layoutBloc.setY(piste.getY()+3);
        RelativeLayout bar = (RelativeLayout) getView().findViewById(R.id.play_time_bar);

        // on remet la barre de lecture au premier plan
        bar.bringToFront();

        // on incrémente le compteur de sample
        this.idSample++;
    }

    /**
     * Affichage de la barre de temps
     */
    public void drawTimeAndPlay() {
        this.timeIndex++;
        if (this.blocks != null && this.barLayout != null) {
            for (Block b : this.blocks) {
                if (b.getStart() == this.timeIndex-1) {
                    this.playTasks.get(b).start();
                    this.playingSample = b.getId();
                }
            }
            this.barLayout.setX(this.timeIndex);
        }
    }

}