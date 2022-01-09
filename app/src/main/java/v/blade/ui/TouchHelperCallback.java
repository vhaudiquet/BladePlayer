package v.blade.ui;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("rawtypes")
public class TouchHelperCallback extends ItemTouchHelper.Callback
{
    private Object[] toOrderObject;
    private List toOrderList;

    public TouchHelperCallback(Object[] toOrder)
    {
        this.toOrderObject = toOrder;
    }

    public TouchHelperCallback(List toOrder)
    {
        this.toOrderList = toOrder;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder)
    {
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        return makeMovementFlags(dragFlags, 0);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target)
    {
        if(toOrderObject == null && toOrderList == null) return false;
        if(recyclerView.getAdapter() == null) return false;

        int from = viewHolder.getAdapterPosition();
        int to = target.getAdapterPosition();

        if(toOrderObject != null)
        {
            Object toMove = toOrderObject[from];
            if(to - from >= 0)
                System.arraycopy(toOrderObject, from + 1, toOrderObject, from, to - from);
            else if(from - to >= 0)
                System.arraycopy(toOrderObject, to, toOrderObject, to + 1, from - to);
            toOrderObject[to] = toMove;
        }
        else Collections.swap(toOrderList, from, to);

        recyclerView.getAdapter().notifyItemMoved(from, to);
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction)
    {
        if(toOrderObject == null && toOrderList == null) return;

        int position = viewHolder.getAdapterPosition();
        if(toOrderObject != null)
        {
            System.arraycopy(toOrderObject, position + 1, toOrderObject, position, toOrderObject.length - position);
            toOrderObject[toOrderObject.length - 1] = null;
        }
        else toOrderList.remove(position);

        //NOTE: Adapter still needs to be notified ; this method needs to be overloaded
    }

    @Override
    public boolean isLongPressDragEnabled()
    {
        //NOTE : we disable long press drag; i find that not intuitive, and it is bugged anyway
        return false;
    }
}
