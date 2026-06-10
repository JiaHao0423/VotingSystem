package com.ben.com.backend.config;

import com.ben.com.backend.domain.entity.AdminUser;
import com.ben.com.backend.domain.entity.Community;
import com.ben.com.backend.domain.entity.Meeting;
import com.ben.com.backend.domain.entity.Unit;
import com.ben.com.backend.domain.enums.AdminRole;
import com.ben.com.backend.domain.enums.BuildingType;
import com.ben.com.backend.domain.enums.MeetingStatus;
import com.ben.com.backend.repository.AdminUserRepository;
import com.ben.com.backend.repository.CommunityRepository;
import com.ben.com.backend.repository.MeetingRepository;
import com.ben.com.backend.repository.UnitRepository;
import com.ben.com.backend.util.UnitShortNameFormatter;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@NullMarked
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
	private final AdminUserRepository adminUserRepository;
	private final PasswordEncoder passwordEncoder;
	private final String defaultAdminUsername;
	private final String defaultAdminPassword;

	public DataInitializer(
			CommunityRepository communityRepository,
			MeetingRepository meetingRepository,
			UnitRepository unitRepository,
			AdminUserRepository adminUserRepository,
			PasswordEncoder passwordEncoder,
			@Value("${spring.security.user.name:admin}") String defaultAdminUsername,
			@Value("${spring.security.user.password:admin}") String defaultAdminPassword
	) {
		this.communityRepository = communityRepository;
		this.meetingRepository = meetingRepository;
		this.unitRepository = unitRepository;
		this.adminUserRepository = adminUserRepository;
		this.passwordEncoder = passwordEncoder;
		this.defaultAdminUsername = defaultAdminUsername;
		this.defaultAdminPassword = defaultAdminPassword;
	}

	@Override
	@Transactional
	public void run(String... args) {
		if (adminUserRepository.count() == 0) {
			adminUserRepository.save(new AdminUser(
					defaultAdminUsername,
					passwordEncoder.encode(defaultAdminPassword),
					"系統管理員",
					AdminRole.SUPER_ADMIN,
					null
			));
			log.info("初始化超級管理員帳號：{}", defaultAdminUsername);
		}

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
