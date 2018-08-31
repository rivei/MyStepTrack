package it.polimi.steptrack.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import it.polimi.steptrack.AppUtils;
import it.polimi.steptrack.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link StatusFragment.OnStatusInteractionListener} interface
 * to handle interaction events.
 * Use the {@link StatusFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StatusFragment extends Fragment
implements SharedPreferences.OnSharedPreferenceChangeListener{
    public final static String TAG = StatusFragment.class.getSimpleName();

    public final static int ON_START_CLICKED = 1;
    public final static int ON_PLACE_CLICKED = 2;

    private Context mContext;

    // TODO: Rename parameter arguments, choose names that match
    private static final String ARG_SESSION_STARTED = "sesssionStarted";
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_HOME_LAT = "homelat";
    private static final String ARG_HOME_LON = "homelon";

    // TODO: Rename and change types of parameters
    private double mHomeLat;
    private double mHomeLon;

    private boolean mSessionStarted = false;
    private int mInteractionType = 0;

    private Button bnOnOff;

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(AppUtils.KEY_STARTING_WALKING_SESSION)) {
            mSessionStarted = AppUtils.startingWalkingSession(mContext);
            setButtonsState();
        }

    }

    private void setButtonsState() {
        if (mSessionStarted) {
            bnOnOff.setText("Stop");
        } else {
            bnOnOff.setText("Start");
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
//    public interface OnSettingFragmentInteractionListener {
//        // TODO: Update argument type and name
//        void onFragmentInteraction(Uri uri);
//    }

    public interface OnStatusInteractionListener {
        void StatusInteraction(int interactionType);
    }

    private OnStatusInteractionListener mListener;

    public StatusFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param homeLat Home latitude.
     * @param homeLon Home longitude.
     * @return A new instance of fragment StatusFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static StatusFragment newInstance(double homeLat, double homeLon) {
        StatusFragment fragment = new StatusFragment();
        Bundle args = new Bundle();
        args.putDouble(ARG_HOME_LAT, homeLat);
        args.putDouble(ARG_HOME_LON, homeLon);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle args = new Bundle();
        args.putBoolean(ARG_SESSION_STARTED, mSessionStarted);
        outState.putAll(args);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mHomeLat = getArguments().getDouble(ARG_HOME_LAT);
            mHomeLon = getArguments().getDouble(ARG_HOME_LON);
        }
        if (mContext != null){
            mSessionStarted = AppUtils.startingWalkingSession(mContext);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_status, container, false);

        TextView tvHome = rootView.findViewById(R.id.tvHome);
        tvHome.setText("Home coordinate: "+ mHomeLat + ", " + mHomeLon);

        bnOnOff = rootView.findViewById(R.id.btnOnOff);
        bnOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.StatusInteraction(ON_START_CLICKED);
                mSessionStarted = true;
            }
        });

        if (savedInstanceState != null){
            mSessionStarted = savedInstanceState.getBoolean(ARG_SESSION_STARTED);
        }
        setButtonsState();

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        if (context instanceof OnStatusInteractionListener) {
            mListener = (OnStatusInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnStatusInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(mContext)
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        PreferenceManager.getDefaultSharedPreferences(mContext)
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }
}
