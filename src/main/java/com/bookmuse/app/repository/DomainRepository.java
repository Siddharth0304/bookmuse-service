package com.bookmuse.app.repository;
import com.bookmuse.app.models.BaseEntity;
import com.bookmuse.app.security.AdministratorVisibility;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
/** Persistence boundary. All dynamic fragments are internal constants; input is always bound. */
@Repository
public class DomainRepository {
    @PersistenceContext private EntityManager em;
    public <T> Optional<T> find(Class<T> type, UUID id) {
        String visibility=AdministratorVisibility.predicate(type);
        if(visibility.isEmpty()) return Optional.ofNullable(em.find(type,id));
        return em.createQuery("select e from "+type.getSimpleName()+" e where e.id=:id and ("+visibility+")",type)
            .setParameter("id",id).getResultStream().findFirst();
    }
    public <T extends BaseEntity> T save(T entity) {
        if (entity.getId()==null) em.persist(entity);
        return entity;
    }
    public void delete(BaseEntity entity) { em.remove(entity); }
    public <T> List<T> list(Class<T> type, String where, Map<String,?> params, int limit) {
        where=visibleWhere(type,where);
        return bind(em.createQuery("select e from "+type.getSimpleName()+" e "+where,type),params)
            .setMaxResults(limit).getResultList();
    }
    public <T> Page<T> page(Class<T> type, String where, Map<String,?> params, Pageable page) {
        if(page.getOffset()>Integer.MAX_VALUE) throw new IllegalArgumentException("Page offset is too large");
        where=visibleWhere(type,where);
        String filter = where.contains("order by ") ? where.substring(0,where.indexOf("order by ")) : where;
        long total = bind(em.createQuery("select count(e) from "+type.getSimpleName()+" e "+filter,Long.class),params).getSingleResult();
        List<T> rows = bind(em.createQuery("select e from "+type.getSimpleName()+" e "+where,type),params)
            .setFirstResult(Math.toIntExact(page.getOffset())).setMaxResults(page.getPageSize()).getResultList();
        return new PageImpl<>(rows,page,total);
    }
    public long count(Class<?> type, String where, Map<String,?> params) {
        where=visibleWhere(type,where);
        return bind(em.createQuery("select count(e) from "+type.getSimpleName()+" e "+where,Long.class),params).getSingleResult();
    }
    public void lockUser(UUID id) {
        var user=em.find(com.bookmuse.app.models.User.class,id);
        if(user!=null) em.refresh(user,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    }
    public Optional<com.bookmuse.app.models.RefreshToken> lockedToken(String hash) {
        return em.createQuery("select e from RefreshToken e where e.tokenHash=:hash",com.bookmuse.app.models.RefreshToken.class)
            .setParameter("hash",hash).setLockMode(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE).getResultStream().findFirst();
    }
    public int execute(String query, Map<String,?> params) {
        var q=em.createQuery(query); params.forEach(q::setParameter); return q.executeUpdate();
    }
    public Map<UUID,Long> counts(Class<?> type,String field,java.util.Collection<UUID> ids,String extra) {
        if(ids.isEmpty()) return Map.of();
        String where=visibleWhere(type,"where e."+field+" in :ids "+extra);
        var rows=em.createQuery("select e."+field+",count(e) from "+type.getSimpleName()+" e "+where+" group by e."+field,Object[].class)
            .setParameter("ids",ids).getResultList();
        Map<UUID,Long> result=new java.util.HashMap<>();
        rows.forEach(row->result.put((UUID)row[0],(Long)row[1])); return result;
    }
    private <T> TypedQuery<T> bind(TypedQuery<T> query,Map<String,?> params) { params.forEach(query::setParameter); return query; }
    private String visibleWhere(Class<?> type,String where) {
        String visibility=AdministratorVisibility.predicate(type);
        if(visibility.isEmpty()) return where;
        int orderIndex=where.indexOf("order by ");
        String filter=orderIndex<0?where:where.substring(0,orderIndex);
        String order=orderIndex<0?"":where.substring(orderIndex);
        String conditions=filter.isBlank()?"":"("+filter.strip().substring("where ".length())+") and ";
        return "where "+conditions+"("+visibility+") "+order;
    }
}
