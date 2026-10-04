package vn.training.books
import android.os.Parcel
import android.os.Parcelable
data class Book(val id: Int,val title: String) : Parcelable {
    override fun writeToParcel(dest: Parcel,flags: Int) { dest.writeInt(id); dest.writeString(title) }
    override fun describeContents()=0
    companion object CREATOR : Parcelable.Creator<Book> {
        override fun createFromParcel(source: Parcel)=Book(source.readInt(),source.readString().orEmpty())
        override fun newArray(size: Int): Array<Book?> = arrayOfNulls(size)
    }
}
