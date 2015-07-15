package upmc.stl.m1.musicstudio.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import upmc.stl.m1.musicstudio.R;
import upmc.stl.m1.musicstudio.tools.StartPointSeekBar;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MixFragment.OnFragmentInteraction} interface
 * to handle interaction events.
 * Use the {@link MixFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MixFragment extends Fragment {

    // Interface de communication avec l'activité et son listener
    private OnFragmentInteraction interactionListener;
    public interface OnFragmentInteraction {
        public void onTracksRequired();
    }

    // Variables locales
    private ArrayList<TrackFragment> tracks;
    private ArrayList<Integer> loaded;
    private HashMap<Integer, TextView> names;
    private HashMap<Integer, LinearLayout> entries;

    public static MixFragment newInstance(String param1, String param2) {
        MixFragment fragment = new MixFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public MixFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.loaded = new ArrayList<Integer>();
        this.names = new HashMap<Integer, TextView>();
        this.entries = new HashMap<Integer, LinearLayout>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mix, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            interactionListener = (OnFragmentInteraction) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement onTracksRequired");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        interactionListener = null;
    }

    /**********************************************************************************************
     *
     * ACCESSEURS ET MODIFICATEURS
     *
     **********************************************************************************************/

    public void setTracks(ArrayList<TrackFragment> tracks) {
        this.tracks = tracks;
    }

    /**********************************************************************************************
     *
     * FONCTIONS DE CREATION DE LA VUE
     *
     **********************************************************************************************/

    /**
     * Fonction de mise à jour des informations de la console
     */
    public void updateConsole() {

        // récupération des pistes
        interactionListener.onTracksRequired();

        // boucle sur les fragments de pistes pour afficher les informations de mixage
        for (TrackFragment f : this.tracks) {
            if (!this.loaded.contains(f.getIdTrack())) {
                this.addTrack(f);
            } else {
                this.names.get(f.getIdTrack()).setText(f.getName());
            }
        }

        // boucle sur les pistes chargée pour voir si certaines ont été supprimées
        boolean contained;
        ArrayList<Integer> clone = (ArrayList<Integer>) this.loaded.clone();
        for (Integer i : clone) {
            contained = false;
            for (TrackFragment f : this.tracks) {
                if (f.getIdTrack() == i) {
                    contained = true;
                    break;
                }
            }
            if (!contained) {
                ((LinearLayout) getActivity().findViewById(R.id.tracks_mix)).removeView(this.entries.get(i));
                this.entries.remove(i);
                this.loaded.remove(i);
                this.names.remove(i);
            }
        }
    }

    /**
     * Fonction d'ajout d'une piste dans la console
     * @param f
     */
    public void addTrack(TrackFragment f) {

        this.loaded.add(f.getIdTrack());
        final int id = f.getIdTrack();

        // récupération du conteneur global de la console
        LinearLayout tracksMix = (LinearLayout) getActivity().findViewById(R.id.tracks_mix);

        // création de la piste de mixage
        LinearLayout track = (LinearLayout) getLayoutInflater(null).inflate(R.layout.mix_console_entry, null);
        this.entries.put(f.getIdTrack(), track);

        // ajout du nom de la piste
        LinearLayout nameLayout = (LinearLayout) track.findViewById(R.id.mix_track_name);
        final TextView name = new TextView(getActivity()); name.setText(f.getName());
        name.setTextSize(20);
        nameLayout.addView(name);
        this.names.put(f.getIdTrack(), name);

        // création de la seekbar de réglage de la balance
        LinearLayout seekBarLayout = (LinearLayout) track.findViewById(R.id.mix_track_seekbar);
        StartPointSeekBar<Integer> seekBar = new StartPointSeekBar<Integer>(-100, +100, getActivity());
        seekBar.setNormalizedValue(0.5);
        seekBar.setLayoutParams(new TableLayout.LayoutParams(50, ViewGroup.LayoutParams.WRAP_CONTENT, 0));
        seekBar.setOnSeekBarChangeListener(new StartPointSeekBar.OnSeekBarChangeListener<Integer>() {
            @Override
            public void onOnSeekBarValueChange(StartPointSeekBar<?> bar, Integer value) {
                float left = 1.0f;
                float right = 1.0f;
                if (value >= -10 && value <= 10) {
                    bar.setNormalizedValue(0.5);
                } else {
                    if (value < 0) {
                        left = Math.abs((float)value / 100);
                        right -= left;
                    }
                    else if (value > 0) {
                        right = Math.abs((float)value / 100);
                        left -= right;
                    }
                }
                for (TrackFragment f : tracks) {
                    if (f.getIdTrack() == id) {
                        f.changeBalance(left, right);
                        break;
                    }
                }
            }
        });
        seekBarLayout.addView(seekBar);

        // ajout de la piste de mixage dans la console
        tracksMix.addView(track);
    }

}