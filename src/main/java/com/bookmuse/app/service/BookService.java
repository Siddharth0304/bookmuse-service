package com.bookmuse.app.service;
import com.bookmuse.app.dto.Requests;
import com.bookmuse.app.dto.Responses;
import com.bookmuse.app.mapper.Views;
import com.bookmuse.app.models.Book;
import com.bookmuse.app.models.LibraryEntry;
import com.bookmuse.app.models.User;
import com.bookmuse.app.repository.DomainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
@Service @RequiredArgsConstructor @Transactional
public class BookService {
    private final DomainRepository repo;
    private final AccessService access;
    @Transactional(readOnly=true)
    public Responses.PageView<Responses.BookView> list(String search,List<String> genres,Pageable page) {
        String where="where 1=1"; Map<String,Object> params=new HashMap<>();
        if(search!=null && !search.isBlank()) { where+=" and (lower(e.title) like :search escape '!' or lower(e.author) like :search escape '!')"; params.put("search","%"+escape(search.toLowerCase(Locale.ROOT))+"%"); }
        Set<String> selectedGenres=new LinkedHashSet<>();
        if(genres!=null) {
            for(String value:genres) {
                if(value==null) continue;
                for(String genre:value.split(",")) {
                    String normalized=genre.strip().toLowerCase(Locale.ROOT);
                    if(!normalized.isBlank()) selectedGenres.add(normalized);
                }
            }
        }
        if(!selectedGenres.isEmpty()) {
            StringJoiner matches=new StringJoiner(" or "," and (",")");
            int index=0;
            for(String genre:selectedGenres) {
                String parameter="genre"+index++;
                matches.add("concat('|',e.genres,'|') like :"+parameter+" escape '!'");
                params.put(parameter,"%|"+escape(genre)+"|%");
            }
            where+=matches.toString();
        }
        return Responses.PageView.of(repo.page(Book.class,where+" order by e.title,e.id",params,page).map(Views::book));
    }
    private String escape(String s) { return s.replace("!","!!").replace("%","!%").replace("_","!_"); }
    @Transactional(readOnly=true) public Responses.BookView get(UUID id) { return Views.book(access.get(Book.class,id)); }
    public Responses.BookView create(Requests.BookInput input) { access.admin(); Book b=new Book(); fill(b,input); repo.save(b); return Views.book(b); }
    public Responses.BookView update(UUID id,Requests.BookInput input) { access.admin(); Book b=access.get(Book.class,id); fill(b,input); return Views.book(b); }
    public void delete(UUID id) { access.admin(); repo.delete(access.get(Book.class,id)); }
    private void fill(Book b,Requests.BookInput r) { b.setTitle(r.title().strip()); b.setAuthor(r.author().strip()); b.setGenres(Views.genres(r.genres())); b.setSummary(r.summary()); b.setCoverUrl(r.coverUrl()); }
    @Transactional(readOnly=true) public Responses.PageView<Responses.LibraryView> library(UUID user,Pageable page) {
        access.get(User.class,user);
        return Responses.PageView.of(repo.page(LibraryEntry.class,"where e.userId=:user order by e.createdAt desc,e.id",Map.of("user",user),page).map(Views::library));
    }
    public Responses.LibraryView putLibrary(UUID book,Requests.LibraryInput r) {
        UUID user=access.id(); repo.lockUser(user); access.get(Book.class,book);
        LibraryEntry e=repo.list(LibraryEntry.class,"where e.userId=:u and e.bookId=:b",Map.of("u",user,"b",book),1).stream().findFirst().orElseGet(LibraryEntry::new);
        e.setUserId(user);e.setBookId(book);e.setStatus(r.status());e.setProgress("READ".equals(r.status())?100:r.progress());repo.save(e);return Views.library(e);
    }
    public void removeLibrary(UUID book) {
        repo.list(LibraryEntry.class,"where e.userId=:u and e.bookId=:b",Map.of("u",access.id(),"b",book),1).forEach(repo::delete);
    }
}
