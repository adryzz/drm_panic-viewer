package gay.nihil.lena.drm_panic_viewer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import java.util.List;

import gay.nihil.lena.drm_panic_viewer.databinding.FragmentFirstBinding;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;

    private RecyclerView recyclerView;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        recyclerView = binding.panicView;

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        MainActivity act = (MainActivity) getActivity();

        LiveData<List<PanicMessage>> live = act.database.panicDao().getRecentEventsLive(200);
        PanicAdapter adapter = new PanicAdapter(getContext(), recyclerView, panic -> {
            Bundle bundle = new Bundle();
            bundle.putParcelable("panic", panic);
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_FirstFragment_to_SecondFragment, bundle);
        });



        live.observe(getViewLifecycleOwner(), pnc -> {
            adapter.setItems(pnc, false);
            recyclerView.smoothScrollToPosition(0);
        });

        recyclerView.setAdapter(adapter);


        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}