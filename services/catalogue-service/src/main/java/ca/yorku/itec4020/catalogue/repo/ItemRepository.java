package ca.yorku.itec4020.catalogue.repo;

import ca.yorku.itec4020.catalogue.entity.ItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItemRepository extends JpaRepository<ItemEntity, Long> {
  @Query("select i from ItemEntity i where lower(i.keywords) like lower(concat('%', :q, '%')) or lower(i.name) like lower(concat('%', :q, '%')) or lower(i.description) like lower(concat('%', :q, '%'))")
  List<ItemEntity> search(@Param("q") String q);
}
