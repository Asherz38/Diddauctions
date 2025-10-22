package ca.yorku.itec4020.auction.repo;

import ca.yorku.itec4020.auction.entity.BidEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BidRepository extends JpaRepository<BidEntity, Long> {
  @Query("select b from BidEntity b where b.itemId = ?1 order by b.createdAt asc, b.id asc")
  List<BidEntity> listByItemId(Long itemId);

  @Query("select b from BidEntity b where b.itemId = ?1 order by b.createdAt desc, b.id desc")
  List<BidEntity> listByItemIdDesc(Long itemId);

  default Optional<BidEntity> findLast(Long itemId) {
    List<BidEntity> list = listByItemIdDesc(itemId);
    return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
  }
}
