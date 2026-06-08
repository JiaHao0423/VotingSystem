package com.ben.com.backend.config;

import com.ben.com.backend.domain.entity.Community;
import com.ben.com.backend.domain.entity.Meeting;
import com.ben.com.backend.domain.entity.Unit;
import com.ben.com.backend.domain.enums.BuildingType;
import com.ben.com.backend.domain.enums.MeetingStatus;
import com.ben.com.backend.repository.CommunityRepository;
import com.ben.com.backend.repository.MeetingRepository;
import com.ben.com.backend.repository.UnitRepository;
import com.ben.com.backend.util.UnitShortNameFormatter;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

	private static final String COMMUNITY_NAME = "鴻邑晴川硯";
	private static final int TOTAL_HOUSEHOLDS = 155;

	private static final List<ShopSeed> SHOPS = List.of(
			new ShopSeed(1, "台中市東區旱溪西路二段196號"),
			new ShopSeed(2, "台中市東區旱溪西路二段195號"),
			new ShopSeed(3, "台中市東區旱溪西路二段193號"),
			new ShopSeed(5, "台中市東區旱溪西路二段190巷2號"),
			new ShopSeed(6, "台中市東區旱溪西路二段190巷6號"),
			new ShopSeed(7, "台中市東區旱溪西路二段190巷12號"),
			new ShopSeed(8, "台中市東區旱溪西路二段190巷16號")
	);

	private final CommunityRepository communityRepository;
	private final MeetingRepository meetingRepository;
	private final UnitRepository unitRepository;

	public DataInitializer(
			CommunityRepository communityRepository,
			MeetingRepository meetingRepository,
			UnitRepository unitRepository
	) {
		this.communityRepository = communityRepository;
		this.meetingRepository = meetingRepository;
		this.unitRepository = unitRepository;
	}

	@Override
	@Transactional
	public void run(String... args) {
		var community = communityRepository.findByName(COMMUNITY_NAME).orElseGet(() -> {
			var created = new Community(
					COMMUNITY_NAME,
					TOTAL_HOUSEHOLDS,
					null,
					"台中市東區旱溪西路二段190巷"
			);
			log.info("初始化社區：{}", COMMUNITY_NAME);
			return communityRepository.save(created);
		});

		if (meetingRepository.findByCommunityIdOrderByMeetingDateDesc(community.getId()).isEmpty()) {
			var meeting = new Meeting(community, "115年度第一次區分所有權人會議", LocalDate.of(2026, 6, 15));
			meeting.setStatus(MeetingStatus.DRAFT);
			meetingRepository.save(meeting);
			log.info("初始化預設區權會場次");
		}

		for (var shop : SHOPS) {
			var shortName = UnitShortNameFormatter.formatShop(shop.shopNo());
			if (unitRepository.existsByCommunityIdAndShortName(community.getId(), shortName)) {
				continue;
			}
			var unit = new Unit(
					community,
					shortName,
					shop.address(),
					BuildingType.SHOP,
					null,
					null,
					shop.shopNo(),
					null,
					null
			);
			unitRepository.save(unit);
			log.info("初始化店面戶別：{}", shortName);
		}
	}

	private record ShopSeed(int shopNo, String address) {
	}
}
