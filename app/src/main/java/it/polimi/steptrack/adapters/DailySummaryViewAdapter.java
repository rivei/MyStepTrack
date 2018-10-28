package it.polimi.steptrack.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import it.polimi.steptrack.R;
import it.polimi.steptrack.roomdatabase.entities.DailySummary;

public class DailySummaryViewAdapter extends RecyclerView.Adapter<DailySummaryViewAdapter.DailySummaryViewHolder>{
    private final LayoutInflater mInflater;
    private List<DailySummary> mReports;

    public DailySummaryViewAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
        mReports = new ArrayList<>();
    }

    @NonNull
    @Override
    public DailySummaryViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View itemView = mInflater.inflate(R.layout.report_item, viewGroup, false);
        return new DailySummaryViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull DailySummaryViewHolder dailySummaryViewHolder, int i) {
        dailySummaryViewHolder.bind(mReports.get(i));
    }


    @Override
    public int getItemCount() {
        if(mReports != null)
            return mReports.size();
        else return  0;
    }

    public void setReports(List<DailySummary> reports) {
        mReports = reports;
        notifyDataSetChanged();
    }

    class DailySummaryViewHolder extends RecyclerView.ViewHolder{

        private TextView tvDate;
        private TextView tvDistance;
        private TextView tvDuration;
        private TextView tvSpeed;
        private TextView tvSteps;

        public DailySummaryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvSpeed = itemView.findViewById(R.id.tvSpeed);
            tvSteps = itemView.findViewById(R.id.tvSteps);
        }

        public void bind(DailySummary report){
            if(report!=null){
                tvDate.setText("Date:" + report.mDate);
                tvSteps.setText("Steps: " + report.steps);
                tvSpeed.setText("Speed: " + report.speed);
                tvDuration.setText("Duration: " + report.walkingduration);
                tvDistance.setText("Distance: " + report.distance);
            }

        }
    }
}
