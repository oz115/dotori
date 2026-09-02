package com.bana.dotori.family.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bana.dotori.family.dto.FamilyCreateRequest;
import com.bana.dotori.family.dto.FamilyCreateResponse;
import com.bana.dotori.family.entity.Family;
import com.bana.dotori.family.entity.FamilyMember;
import com.bana.dotori.family.mapper.FamilyMapper;
import com.bana.dotori.family.repository.FamilyMemberRepository;
import com.bana.dotori.family.repository.FamilyRepository;
import com.bana.dotori.global.error.BusinessException;
import com.bana.dotori.global.error.ErrorCode;
import com.bana.dotori.user.entity.User;
import com.bana.dotori.user.entity.UserRole;
import com.bana.dotori.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class FamilyServiceImplTest {

	@Mock
	private FamilyRepository familyRepository;

	@Mock
	private FamilyMemberRepository familyMemberRepository;

	@Mock
	private UserRepository userRepository;

	@Spy
	private FamilyMapper familyMapper;

	@InjectMocks
	private FamilyServiceImpl familyService;

	private User parent() {
		return User.builder()
			.loginId("parent01")
			.password("encoded-password")
			.name("김도토")
			.email("parent@example.com")
			.role(UserRole.PARENT)
			.build();
	}

	private User child() {
		return User.builder()
			.loginId("child01")
			.password("encoded-password")
			.name("김도랑")
			.email("child@example.com")
			.role(UserRole.CHILD)
			.build();
	}

	@Test
	@DisplayName("가족 생성 성공 시 생성된 가족 정보를 반환하고 생성자를 구성원으로 등록한다")
	void create_success() {
		given(userRepository.findById(1)).willReturn(Optional.of(parent()));
		given(familyMemberRepository.existsByUserId(1)).willReturn(false);
		given(familyRepository.save(any(Family.class))).willAnswer(invocation -> invocation.getArgument(0));

		FamilyCreateResponse response = familyService.create(1, new FamilyCreateRequest("도토리네 가족"));

		assertThat(response.name()).isEqualTo("도토리네 가족");
		assertThat(response.createdBy()).isEqualTo(1);
		verify(familyMemberRepository).save(any(FamilyMember.class));
	}

	@Test
	@DisplayName("부모 권한이 아니면 FORBIDDEN 예외가 발생한다")
	void create_notParent() {
		given(userRepository.findById(2)).willReturn(Optional.of(child()));

		assertThatThrownBy(() -> familyService.create(2, new FamilyCreateRequest("도토리네 가족")))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FORBIDDEN);
	}

	@Test
	@DisplayName("이미 가족에 소속되어 있으면 ALREADY_IN_FAMILY 예외가 발생한다")
	void create_alreadyInFamily() {
		given(userRepository.findById(1)).willReturn(Optional.of(parent()));
		given(familyMemberRepository.existsByUserId(1)).willReturn(true);

		assertThatThrownBy(() -> familyService.create(1, new FamilyCreateRequest("도토리네 가족")))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.ALREADY_IN_FAMILY);
	}

	@Test
	@DisplayName("토큰의 유저가 존재하지 않으면 UNAUTHORIZED 예외가 발생한다")
	void create_userNotFound() {
		given(userRepository.findById(99)).willReturn(Optional.empty());

		assertThatThrownBy(() -> familyService.create(99, new FamilyCreateRequest("도토리네 가족")))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.UNAUTHORIZED);
	}
}
