package in.rajalakshmi.record;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

class NotesViewHolder extends RecyclerView.ViewHolder {
    View mview;
    public NotesViewHolder(@NonNull View itemView) {
        super(itemView);
        mview=itemView;
    }
    void setname(String name){
        TextView title=mview.findViewById(R.id.data);
        title.setText(name);
    }
}
