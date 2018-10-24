package it.polimi.steptrack.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import it.polimi.steptrack.R;
import it.polimi.steptrack.roomdatabase.entities.WalkingSession;
//import it.polimi.steptrack.ui.WalkingSessionFragment.OnListFragmentInteractionListener;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link WalkingSession} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class WalkingSessionRecyclerViewAdapter extends RecyclerView.Adapter<WalkingSessionRecyclerViewAdapter.ViewHolder> {

    private final LayoutInflater mInflater;
    private List<WalkingSession> mWalkingSessions;
    //private final OnListFragmentInteractionListener mListener;
    private Context mContext;

//    public WalkingSessionRecyclerViewAdapter(List<WalkingSession> items, OnListFragmentInteractionListener listener) {
//        this.mWalkingSessions = items;
//        //mListener = listener;
//        this.mContext = context;
//    }

    public WalkingSessionRecyclerViewAdapter(Context context, List<WalkingSession> walkingSessions){
        this.mInflater = LayoutInflater.from(context);
        this.mWalkingSessions = walkingSessions;//new ArrayList<>();
        //this.mWalkingSessions = walkingSessions;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.fragment_walkingsession, parent, false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        /*        WalkingSession walkingSession = walkingSessions.get(position);
        holder.dateView.setText(str);

        str = String.format("Duration: %1.1f Minutes", walkingSession.getmDuration()/60000f);
        holder.durationView.setText(str);

        str = "Step Count: " + String.valueOf(walkingSession.getmStepCount());
        holder.stepsView.setText(str);*/

        if(mWalkingSessions!=null)
            holder.bind(mWalkingSessions.get(position));
        else holder.mIdView.setText("no Session");
//        holder.mWalkingSession = mWalkingSessions.get(position);
//        holder.mIdView.setText(String.format("Session: %d",mWalkingSessions.get(position).sid));
//        Date ssdate = new Date(holder.mWalkingSession.mStartTime);
//        String str;
//        str = "Session date: " + DateFormat.getDateInstance().format(ssdate);
//        holder.mTimeView.setText(str);
//        holder.mDurationView.setText("Duration: " + mWalkingSessions.get(position).mDuration/1000 + " s");
//        holder.mDistanceView.setText("Distance: " + mWalkingSessions.get(position).mDistance + "m");
//        holder.mSpeedView.setText("Average speed: " + mWalkingSessions.get(position).mAverageSpeed + "m/s");

//        holder.mView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (null != mListener) {
//                    // Notify the active callbacks interface (the activity, if the
//                    // fragment is attached to one) that an item has been selected.
//                    mListener.onListFragmentInteraction(holder.mWalkingSession);
//                }
//            }
//        });
    }

    @Override
    public int getItemCount() {
        if(mWalkingSessions!= null)
            return mWalkingSessions.size();
        else return 0;
    }

    public void setSessions(List<WalkingSession> walkingSessions){
        mWalkingSessions = walkingSessions;
        notifyDataSetChanged(); //The key to show data
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView mIdView;
        private final TextView mTimeView;
        private final TextView mDurationView;
        private final TextView mDistanceView;
        private final TextView mSpeedView;

        private ViewHolder(View view) {
            super(view);
            mIdView = (TextView) view.findViewById(R.id.tvId);
            mTimeView = (TextView) view.findViewById(R.id.tvTime);
            mDurationView = view.findViewById(R.id.tvDuration);
            mDistanceView = view.findViewById(R.id.tvDistance);
            mSpeedView = view.findViewById(R.id.tvSpeed);
        }

        private void bind(final WalkingSession walkingSession){
            if(walkingSession != null){
                mIdView.setText(String.format("Session: %d",walkingSession.sid));
                Date ssdate = new Date(walkingSession.mStartTime);
                String str;
                str = "Session date: " + DateFormat.getDateInstance().format(ssdate);
                mTimeView.setText(str);
                mDurationView.setText("Duration: " + walkingSession.mDuration/1000 + " s");
                mDistanceView.setText("Distance: " + walkingSession.mDistance + "m");
                mSpeedView.setText("Average speed: " + walkingSession.mAverageSpeed + "m/s");
            }
            else {
                mIdView.setText("no session");
            }
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mTimeView.getText() + "'";
        }
    }
}
