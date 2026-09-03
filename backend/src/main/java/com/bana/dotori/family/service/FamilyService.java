package com.bana.dotori.family.service;

import java.util.List;

import com.bana.dotori.family.dto.ConnectedChild;
import com.bana.dotori.family.dto.FamilyConnectRequest;
import com.bana.dotori.family.dto.FamilyConnectResponse;
import com.bana.dotori.family.dto.FamilyCreateRequest;
import com.bana.dotori.family.dto.FamilyCreateResponse;
import com.bana.dotori.family.dto.FamilyMembersResponse;
import com.bana.dotori.family.dto.FamilyResponse;
import com.bana.dotori.family.dto.InvitationCodeResponse;

public interface FamilyService {

	FamilyCreateResponse create(Integer userId, FamilyCreateRequest request);

	FamilyResponse getFamily(Integer userId, Integer familyId);

	FamilyMembersResponse getMembers(Integer userId, Integer familyId);

	void deleteFamily(Integer userId, Integer familyId);

	void removeChild(Integer userId, Integer familyId, Integer childId);

	InvitationCodeResponse createInvitationCode(Integer userId, Integer familyId);

	FamilyConnectResponse connect(Integer userId, FamilyConnectRequest request);

	/**
	 * 해당 부모와 같은 가족에 속한 자녀 목록을 합류 순으로 돌려준다.
	 *
	 * <p>마이페이지의 연결된 자녀 조회(MYPAGE-06)가 쓴다. 가족이 없으면 빈 목록이다.
	 */
	List<ConnectedChild> findConnectedChildren(Integer parentUserId);
}
