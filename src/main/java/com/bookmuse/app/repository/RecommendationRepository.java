package com.bookmuse.app.repository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import com.bookmuse.app.models.Book;
import java.util.UUID;
@Repository
public class RecommendationRepository {
    @PersistenceContext private EntityManager em;
    /** Each favourite genre contributes 2 points; each library book contributes 1 per matching genre.
     * Sum candidate genre weights, then break ties by library popularity and ID for stable pagination.
     * Empty affinities naturally fall back to popularity. Keep this SQL in one place for future tuning.
     */
    private static final String SQL="""
        with affinity as (
            select genre,sum(weight) weight from (
                select unnest(string_to_array(favourite_genres,'|')) genre,2 weight from users where id=:user
                union all
                select unnest(string_to_array(b.genres,'|')) genre,1 weight
                from books b join library_entries l on l.book_id=b.id where l.user_id=:user
            ) a where genre<>'' group by genre
        ), popularity as (select book_id,count(*) total from library_entries group by book_id)
        select b.* from books b left join popularity p on p.book_id=b.id
        where not exists (select 1 from library_entries l where l.book_id=b.id and l.user_id=:user)
        order by (select coalesce(sum(a.weight),0) from affinity a where a.genre=any(string_to_array(b.genres,'|'))) desc,
            coalesce(p.total,0) desc,b.id
        """;
    @SuppressWarnings("unchecked")
    public Page<Book> recommend(UUID user,Pageable page) {
        if(page.getOffset()>Integer.MAX_VALUE) throw new IllegalArgumentException("Page offset is too large");
        var rows=em.createNativeQuery(SQL,Book.class).setParameter("user",user)
            .setFirstResult(Math.toIntExact(page.getOffset())).setMaxResults(page.getPageSize()).getResultList();
        long count=((Number)em.createNativeQuery("select count(*) from books b where not exists (select 1 from library_entries l where l.book_id=b.id and l.user_id=:user)").setParameter("user",user).getSingleResult()).longValue();
        return new PageImpl<Book>(rows,page,count);
    }
}
