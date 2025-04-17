package gay.nihil.lena.drm_panic_viewer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

class PanicAdapter extends RecyclerView.Adapter<PanicAdapter.PanicViewHolder> {
    List<PanicMessage> events;
    Context context;
    PanicViewClickHandler handler;

    RecyclerView recyclerView;
    long lastTime = Long.MAX_VALUE;

    public PanicAdapter(Context ctx, RecyclerView view, PanicViewClickHandler hdl){
        events = new ArrayList<PanicMessage>();
        context = ctx;
        recyclerView = view;
        handler = hdl;
    }

    @NonNull
    @Override
    public PanicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.panic_card, parent, false);


        return new PanicViewHolder(view, handler);
    }

    @Override
    public void onBindViewHolder(@NonNull PanicViewHolder holder, int position) {
        // Bind data for the Panic item to the views in the PanicViewHolder
        if (events == null) {
            return;
        }

        PanicMessage event = events.get(position);

        holder.name.setText(event.reason);
        holder.text.setText(event.version);
        holder.timestamp.setText(Utils.timestampToText(event.timestamp));
        holder.msg = event;

        lastTime = event.timestamp;
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public void setItems(List<PanicMessage> evt, boolean fullUpdate) {
        events = evt;

        if (fullUpdate) {
            notifyDataSetChanged();
        } else {
            notifyItemInserted(0);
        }
    }

    static class PanicViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView text;
        TextView timestamp;
        PanicMessage msg;

        PanicViewHolder(View itemView, PanicViewClickHandler handler) {
            super(itemView);
            name = itemView.findViewById(R.id.name_text);
            text = itemView.findViewById(R.id.description_text);
            timestamp = itemView.findViewById(R.id.timestamp_text);

            itemView.setOnClickListener(v -> {
                handler.click(msg);
            });

            itemView.setOnLongClickListener(v -> {
                Snackbar.make(itemView, "This should open a menu or something idk", Snackbar.LENGTH_LONG).show();
                return true;
            });
        }
    }

    public interface PanicViewClickHandler {
        public void click(PanicMessage msg);
    }

}