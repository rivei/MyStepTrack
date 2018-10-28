package it.polimi.steptrack.ui;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import it.polimi.steptrack.R;
import it.polimi.steptrack.adapters.DailySummaryViewAdapter;
import it.polimi.steptrack.roomdatabase.entities.DailySummary;
import it.polimi.steptrack.viewmodels.ReportViewModel;

public class ReportFragment extends Fragment {

    public static final String TAG = ReportFragment.class.getSimpleName();

    private ReportViewModel mViewModel;
//    private DailySummaryViewAdapter mAdapter;

    public static ReportFragment newInstance() {
        return new ReportFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.report_fragment, container, false);


        RecyclerView recyclerView = rootView.findViewById(R.id.reportlist);
        recyclerView.setLayoutManager(new LinearLayoutManager(rootView.getContext(), LinearLayoutManager.VERTICAL, false));
        final DailySummaryViewAdapter mAdapter = new DailySummaryViewAdapter(rootView.getContext());
        mViewModel = ViewModelProviders.of(this).get(ReportViewModel.class);
        // TODO: Use the ViewModel
        mViewModel.getAllReports().observe(this, new Observer<List<DailySummary>>() {
            @Override
            public void onChanged(@Nullable List<DailySummary> dailySummaries) {
                mAdapter.setReports(dailySummaries);
            }
        });

        recyclerView.setAdapter(mAdapter);

        return rootView;
    }

//    @Override
//    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
//        super.onActivityCreated(savedInstanceState);
//        mViewModel = ViewModelProviders.of(this).get(ReportViewModel.class);
//        // TODO: Use the ViewModel
//        mViewModel.getAllReports().observe(this, new Observer<List<DailySummary>>() {
//            @Override
//            public void onChanged(@Nullable List<DailySummary> dailySummaries) {
//                mAdapter.setReports(dailySummaries);
//            }
//        });
//    }

}
