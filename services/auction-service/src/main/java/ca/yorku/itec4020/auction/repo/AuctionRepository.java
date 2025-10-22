package ca.yorku.itec4020.auction.repo;

import ca.yorku.itec4020.auction.entity.AuctionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionRepository extends JpaRepository<AuctionEntity, Long> {
}
