package org.commcare.views.media;

/**
 * Used to represent the unique id of views in a ListAdapter and form entry
 *
 * @author amstone326
 */
public class ViewId {

    private final long rowId;
    private final long colId;
    private final boolean isDetail;

    private ViewId(long rowId, long colId, boolean isDetail) {
        this.rowId = rowId;
        this.colId = colId;
        this.isDetail = isDetail;
    }

    public static ViewId buildTableViewId(long rowId, long colId, boolean isDetail) {
        return new ViewId(rowId, colId, isDetail);
    }

    public static ViewId buildListViewId(long position) {
        return new ViewId(position, 0, false);
    }

    @Override
    public String toString() {
        return "(" + rowId + "," + colId + "," + isDetail + ")";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int)(colId ^ (colId >>> 32));
        result = prime * result + (isDetail ? 1231 : 1237);
        result = prime * result + (int)(rowId ^ (rowId >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || !(obj instanceof ViewId)) {
            return false;
        } else {
            ViewId other = (ViewId)obj;

            return colId == other.colId
                    && isDetail == other.isDetail
                    && rowId == other.rowId;
        }
    }
}
