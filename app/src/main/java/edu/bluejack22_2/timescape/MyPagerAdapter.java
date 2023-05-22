package edu.bluejack22_2.timescape;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

import edu.bluejack22_2.timescape.model.Message;

public class MyPagerAdapter extends RecyclerView.Adapter<MyPagerAdapter.ViewHolder> {

    private List<Message> messageList;
    private Context context;
    private FirebaseStorage storage;

    public MyPagerAdapter(Context context, List<Message> messageList) {
        this.context = context;
        this.messageList = messageList;
        this.storage = FirebaseStorage.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.pager_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message message = messageList.get(position);

        StorageReference storageRef = storage.getReferenceFromUrl(message.getContent());
        storageRef.getMetadata().addOnSuccessListener(storageMetadata -> {
            String mimeType = storageMetadata.getContentType();
            if (mimeType != null && mimeType.startsWith("video")) {
                holder.playerView.setVisibility(View.VISIBLE);
                holder.fullScreenImageView.setVisibility(View.GONE);
                ExoPlayer player = new ExoPlayer.Builder(context).build();
                holder.playerView.setPlayer(player);

                MediaItem mediaItem = MediaItem.fromUri(message.getContent());
                player.setMediaItem(mediaItem);
                player.prepare();
                player.play();
            } else {
                holder.playerView.setVisibility(View.GONE);
                holder.fullScreenImageView.setVisibility(View.VISIBLE);
                Glide.with(context).load(message.getContent()).into(holder.fullScreenImageView);
            }
        }).addOnFailureListener(e -> {
            Log.e("MyPagerAdapter", "Failed to fetch mime type", e);
            holder.playerView.setVisibility(View.GONE);
            Glide.with(context).load(message.getContent()).into(holder.fullScreenImageView);
        });

        holder.itemView.setOnClickListener(v -> ((FullScreenImageActivity) context).toggleUIVisibility());
        holder.playerView.setOnClickListener(v -> ((FullScreenImageActivity) context).toggleUIVisibility());
        holder.fullScreenImageView.setOnClickListener(v -> ((FullScreenImageActivity) context).toggleUIVisibility());
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        PhotoView fullScreenImageView;
        StyledPlayerView playerView;

        ViewHolder(View itemView) {
            super(itemView);
            fullScreenImageView = itemView.findViewById(R.id.fullScreenImageView);
            playerView = itemView.findViewById(R.id.player_view);
        }
    }
}

