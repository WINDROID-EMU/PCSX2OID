package kr.co.iefriends.pcsx2.input.view;

public interface EditableControl {
    boolean isPointInside(float x, float y);
    void setEditMode(boolean editMode);
}
