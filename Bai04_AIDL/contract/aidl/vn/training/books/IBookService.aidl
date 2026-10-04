package vn.training.books;
import vn.training.books.Book;
import vn.training.books.IBookListener;
interface IBookService {
    void addBook(in Book book);
    List<Book> getBooks();
    int getBookCount();
    void registerListener(IBookListener listener);
    void unregisterListener(IBookListener listener);
    boolean removeBook(int id);
}
