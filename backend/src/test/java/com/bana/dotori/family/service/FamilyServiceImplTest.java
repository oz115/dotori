package com.bana.dotori.family.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
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
import com.bana.dotori.family.dto.FamilyMembersResponse;
import com.bana.dotori.family.dto.FamilyResponse;
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
	@DisplayName("가족 조회 성공 시 가족 정보와 구성원 수를 반환한다")
	void getFamily_success() {
		Family family = Family.builder().name("도토리네 가족").createdBy(1).build();
		given(familyRepository.findById(1)).willReturn(Optional.of(family));
		given(familyMemberRepository.existsByFamilyIdAndUserId(1, 1)).willReturn(true);
		given(familyMemberRepository.countByFamilyId(1)).willReturn(3L);

		FamilyResponse response = familyService.getFamily(1, 1);

		assertThat(response.name()).isEqualTo("도토리네 가족");
		assertThat(response.createdBy()).isEqualTo(1);
		assertThat(response.memberCount()).isEqualTo(3);
	}

	@Test
	@DisplayName("가족이 존재하지 않으면 FAMILY_NOT_FOUND 예외가 발생한다")
	void getFamily_familyNotFound() {
		given(familyRepository.findById(99)).willReturn(Optional.empty());

		assertThatThrownBy(() -> familyService.getFamily(1, 99))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FAMILY_NOT_FOUND);
	}

	@Test
	@DisplayName("소속되지 않은 가족을 조회하면 FAMILY_NOT_FOUND 예외가 발생한다")
	void getFamily_notMember() {
		Family family = Family.builder().name("도토리네 가족").createdBy(1).build();
		given(familyRepository.findById(1)).willReturn(Optional.of(family));
		given(familyMemberRepository.existsByFamilyIdAndUserId(1, 2)).willReturn(false);

		assertThatThrownBy(() -> familyService.getFamily(2, 1))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FAMILY_NOT_FOUND);
	}

	@Test
	@DisplayName("구성원 조회 성공 시 구성원 목록을 반환한다")
	void getMembers_success() {
		Family family = Family.builder().name("도토리네 가족").createdBy(1).build();
		FamilyMember parentMember = FamilyMember.builder().family(family).user(parent()).build();
		FamilyMember childMember = FamilyMember.builder().family(family).user(child()).build();
		given(familyRepository.findById(1)).willReturn(Optional.of(family));
		given(familyMemberRepository.existsByFamilyIdAndUserId(1, 1)).willReturn(true);
		given(familyMemberRepository.findAllWithUserByFamilyId(1))
			.willReturn(List.of(parentMember, childMember));

		FamilyMembersResponse response = familyService.getMembers(1, 1);

		assertThat(response.members()).hasSize(2);
		assertThat(response.members().get(0).name()).isEqualTo("김도토");
		assertThat(response.members().get(0).role()).isEqualTo(UserRole.PARENT);
		assertThat(response.members().get(1).name()).isEqualTo("김도랑");
		assertThat(response.members().get(1).role()).isEqualTo(UserRole.CHILD);
	}

	@Test
	@DisplayName("소속되지 않은 가족의 구성원을 조회하면 FAMILY_NOT_FOUND 예외가 발생한다")
	void getMembers_notMember() {
		Family family = Family.builder().name("도토리네 가족").createdBy(1).build();
		given(familyRepository.findById(1)).willReturn(Optional.of(family));
		given(familyMemberRepository.existsByFamilyIdAndUserId(1, 2)).willReturn(false);

		assertThatThrownBy(() -> familyService.getMembers(2, 1))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FAMILY_NOT_FOUND);
	}

	@Test
	@DisplayName("가족 해체 성공 시 구성원과 가족을 모두 삭제한다")
	void disband_success() {
		Family family = Family.builder().name("도토리네 가족").createdBy(1).build();
		given(userRepository.findById(1)).willReturn(Optional.of(parent()));
		given(familyRepository.findById(1)).willReturn(Optional.of(family));
		given(familyMemberRepository.existsByFamilyIdAndUserId(1, 1)).willReturn(true);

		familyService.disband(1, 1);

		verify(familyMemberRepository).deleteAllByFamilyId(1);
		verify(familyRepository).delete(family);
	}

	@Test
	@DisplayName("부모 권한이 아니면 가족 해체 시 FORBIDDEN 예외가 발생한다")
	void disband_notParent() {
		given(userRepository.findById(2)).willReturn(Optional.of(child()));

		assertThatThrownBy(() -> familyService.disband(2, 1))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FORBIDDEN);
	}

	@Test
	@DisplayName("소속되지 않은 가족을 해체하려 하면 FORBIDDEN 예외가 발생한다")
	void disband_notMember() {
		Family family = Family.builder().name("남의 가족").createdBy(3).build();
		given(userRepository.findById(1)).willReturn(Optional.of(parent()));
		given(familyRepository.findById(1)).willReturn(Optional.of(family));
		given(familyMemberRepository.existsByFamilyIdAndUserId(1, 1)).willReturn(false);

		assertThatThrownBy(() -> familyService.disband(1, 1))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FORBIDDEN);
	}

	@Test
	@DisplayName("존재하지 않는 가족을 해체하려 하면 FAMILY_NOT_FOUND 예외가 발생한다")
	void disband_familyNotFound() {
		given(userRepository.findById(1)).willReturn(Optional.of(parent()));
		given(familyRepository.findById(99)).willReturn(Optional.empty());

		assertThatThrownBy(() -> familyService.disband(1, 99))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FAMILY_NOT_FOUND);
	}

	@Test
	@DisplayName("자녀 제외 성공 시 자녀 구성원을 삭제한다")
	void removeChild_success() {
		Family family = Family.builder().name("도토리네 가족").createdBy(1).build();
		FamilyMember childMember = FamilyMember.builder().family(family).user(child()).build();
		given(userRepository.findById(1)).willReturn(Optional.of(parent()));
		given(familyMemberRepository.existsByFamilyIdAndUserId(1, 1)).willReturn(true);
		given(familyMemberRepository.findWithUserByFamilyIdAndUserId(1, 2)).willReturn(Optional.of(childMember));

		familyService.removeChild(1, 1, 2);

		verify(familyMemberRepository).delete(childMember);
	}

	@Test
	@DisplayName("부모 권한이 아니면 자녀 제외 시 FORBIDDEN 예외가 발생한다")
	void removeChild_notParent() {
		given(userRepository.findById(2)).willReturn(Optional.of(child()));

		assertThatThrownBy(() -> familyService.removeChild(2, 1, 3))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FORBIDDEN);
	}

	@Test
	@DisplayName("해당 자녀가 가족에 없으면 MEMBER_NOT_FOUND 예외가 발생한다")
	void removeChild_memberNotFound() {
		given(userRepository.findById(1)).willReturn(Optional.of(parent()));
		given(familyMemberRepository.existsByFamilyIdAndUserId(1, 1)).willReturn(true);
		given(familyMemberRepository.findWithUserByFamilyIdAndUserId(1, 99)).willReturn(Optional.empty());

		assertThatThrownBy(() -> familyService.removeChild(1, 1, 99))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
	}

	@Test
	@DisplayName("제외 대상이 자녀가 아니면 MEMBER_NOT_FOUND 예외가 발생한다")
	void removeChild_targetNotChild() {
		Family family = Family.builder().name("도토리네 가족").createdBy(1).build();
		FamilyMember parentMember = FamilyMember.builder().family(family).user(parent()).build();
		given(userRepository.findById(1)).willReturn(Optional.of(parent()));
		given(familyMemberRepository.existsByFamilyIdAndUserId(1, 1)).willReturn(true);
		given(familyMemberRepository.findWithUserByFamilyIdAndUserId(1, 2)).willReturn(Optional.of(parentMember));

		assertThatThrownBy(() -> familyService.removeChild(1, 1, 2))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
	}

	@Test
	@DisplayName("소속되지 않은 가족에서 자녀 제외를 시도하면 FORBIDDEN 예외가 발생한다")
	void removeChild_notMember() {
		given(userRepository.findById(1)).willReturn(Optional.of(parent()));
		given(familyMemberRepository.existsByFamilyIdAndUserId(1, 1)).willReturn(false);

		assertThatThrownBy(() -> familyService.removeChild(1, 1, 2))
			.isInstanceOf(BusinessException.class)
			.extracting(e -> ((BusinessException)e).getErrorCode())
			.isEqualTo(ErrorCode.FORBIDDEN);
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
