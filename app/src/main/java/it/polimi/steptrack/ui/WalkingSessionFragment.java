package it.polimi.steptrack.ui;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import it.polimi.steptrack.R;
import it.polimi.steptrack.adapters.WalkingSessionRecyclerViewAdapter;
import it.polimi.steptrack.roomdatabase.entities.WalkingSession;
import it.polimi.steptrack.viewmodels.WalkingSessionViewModel;


public class WalkingSessionFragment extends Fragment {
    public final static String TAG = WalkingSessionFragment.class.getSimpleName();

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
//    private OnListFragmentInteractionListener mListener;

    private WalkingSessionViewModel mWalkingSessionViewModel;
    private Context mContext;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public WalkingSessionFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static WalkingSessionFragment newInstance(int columnCount) {
        WalkingSessionFragment fragment = new WalkingSessionFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_walkingsession_list, container, false);
        mContext = rootView.getContext();

        // Set the adapter
//        if (rootView instanceof RecyclerView) {
            Context context = rootView.getContext();
//            RecyclerView recyclerView = (RecyclerView) rootView;
//            if (mColumnCount <= 1) {
//                recyclerView.setLayoutManager(new LinearLayoutManager(context));
//            } else {
//                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
//            }

            RecyclerView recyclerView = rootView.findViewById(R.id.list);
            recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
            final WalkingSessionRecyclerViewAdapter adapter = new WalkingSessionRecyclerViewAdapter(mContext, new ArrayList<>());
            // Get a new or existing ViewModel from the ViewModelProvider.
            mWalkingSessionViewModel = ViewModelProviders.of(this).get(WalkingSessionViewModel.class);
            // Add an observer on the LiveData returned by getAlphabetizedWords.
            // The onChanged() method fires when the observed data changes and the activity is
            // in the foreground.
            mWalkingSessionViewModel.getAllSessions().observe(this, new Observer<List<WalkingSession>>() {
                @Override
                public void onChanged(@Nullable final List<WalkingSession> walkingSessions) {
                    // Update the cached copy of the words in the adapter.
                    adapter.setSessions(walkingSessions);
                }
            });
            recyclerView.setAdapter(adapter);

//        }
        return rootView;
    }


//    @Override
//    public void onAttach(Context context) {
//        super.onAttach(context);
//        if (context instanceof OnListFragmentInteractionListener) {
//            mListener = (OnListFragmentInteractionListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnListFragmentInteractionListener");
//        }
//    }
//
//    @Override
//    public void onDetach() {
//        super.onDetach();
//        mListener = null;
//    }
//
//    /**
//     * This interface must be implemented by activities that contain this
//     * fragment to allow an interaction in this fragment to be communicated
//     * to the activity and potentially other fragments contained in that
//     * activity.
//     * <p/>
//     * See the Android Training lesson <a href=
//     * "http://developer.android.com/training/basics/fragments/communicating.html"
//     * >Communicating with Other Fragments</a> for more information.
//     */
//    public interface OnListFragmentInteractionListener {
//        // TODO: Update argument type and name
//        void onListFragmentInteraction(WalkingSession walkingSession);
//    }
}
