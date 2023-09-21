package com.ssafy.share.service;

import com.ssafy.share.api.request.ShareBoardUpdateRequest;
import com.ssafy.share.api.request.ShareBoardWriteRequest;
import com.ssafy.share.api.request.ShareIngredientRequest;
import com.ssafy.share.api.response.MemberResponse;
import com.ssafy.share.db.entity.LocationInfo;
import com.ssafy.share.db.entity.ShareImage;
import com.ssafy.share.db.entity.ShareIngredient;
import com.ssafy.share.db.entity.SharePost;
import com.ssafy.share.db.repository.LocationInfoRepository;
import com.ssafy.share.db.repository.MemberRepository;
import com.ssafy.share.db.repository.ShareBoardRepository;
import com.ssafy.share.db.repository.ShareIngredientRepository;
import com.ssafy.share.feign.MemberFeign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShareBoardServiceImpl implements ShareBoardService {

    private final ShareBoardRepository shareBoardRepository;
    private final ShareIngredientRepository shareIngredientRepository;
    private final LocationInfoRepository locationInfoRepository;
    private final MemberFeign memberFeign;
    @Override
    public MemberResponse getMember(Long memberId) {
        MemberResponse memberResponse = memberFeign.getMemberDetail(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다. ID: "+memberId));
        return memberResponse;
    }
    @Override
    public SharePost findBySharePostId(Long shareBoardId) { // id로 나눔글 1건 조회
        return shareBoardRepository.findBySharePostId(shareBoardId)
                .orElseThrow(() -> new IllegalArgumentException("해당하는 게시글을 찾을 수 없습니다. ID: " + shareBoardId));
    }

    @Override
    public Page<SharePost> getPostList(Pageable pageable,LocationInfo locationInfo, String keyword) { // 나눔글 리스트 조회
        log.info("검색 키워드: {}",keyword);
        // todo: 시간을 n분전 형식으로 바꿔야함
        if(keyword==null) return shareBoardRepository.findByLocationInfo(pageable,locationInfo); // 초기화면 or 검색어 없을 때 전체 조회
        return shareBoardRepository.findByLocationInfoAndTitleContaining(pageable,locationInfo,keyword); // 검색어를 입력했을 때
    }

    @Override
    public SharePost getPostDetail(Long shareBoardId) {
        return shareBoardRepository.findBySharePostId(shareBoardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. ID: " + shareBoardId));

    }

    @Override
    @Transactional
    public SharePost save(List<MultipartFile> imageFiles, List<ShareIngredientRequest> shareIngredientRequests,
                          ShareBoardWriteRequest shareBoardWriteRequest) { // 나눔글 등록
        List<ShareImage> images=null;

        if(imageFiles != null){
            shareBoardWriteRequest.setShareImages(images);
            // todo: 이미지에 url을 뽑아내서 저장해야함
        }

        Short locationId=shareBoardWriteRequest.getLocationId();
        LocationInfo locationInfo=locationInfoRepository.findByLocationId(locationId)
                        .orElseThrow(() -> new IllegalArgumentException("장소를 찾을 수 없습니다. ID: " + locationId));
        shareBoardWriteRequest.setLocationInfo(locationInfo); // dto에 지역 설정
        SharePost post=shareBoardWriteRequest.toEntity(); // post 엔티티 생성
        for(ShareIngredientRequest s:shareIngredientRequests){ // 나눔식재료 하나하나 post 등록
            s.setSharePost(post);
            ShareIngredient shareIngredient=s.toEntity();
            shareBoardWriteRequest.getShareIngredients().add(shareIngredient);
        }
        return shareBoardRepository.save(post);
    }

    @Override
    @Transactional
    public SharePost update(Long shareBoardId, List<MultipartFile> imageFiles, List<ShareIngredientRequest> shareIngredientRequests,
                            ShareBoardUpdateRequest shareBoardUpdateRequest) {

        SharePost post=shareBoardRepository.findBySharePostId(shareBoardId)
                .orElseThrow(() -> new IllegalArgumentException("해당하는 게시글을 찾을 수 없습니다. ID: " + shareBoardId));

        List<ShareImage> images=null;
        if(imageFiles != null){
            shareBoardUpdateRequest.setShareImages(images);
            // todo: 이미지에 url을 뽑아내서 저장해야함
        }

        Short locationId=shareBoardUpdateRequest.getLocationId();
        LocationInfo locationInfo=locationInfoRepository.findByLocationId(locationId)
                .orElseThrow(() -> new IllegalArgumentException("장소를 찾을 수 없습니다. ID: " + locationId));
        shareBoardUpdateRequest.setLocationInfo(locationInfo);


        // 기존의 나눔식재료는 삭제
        shareIngredientRepository.deleteBySharePost(post);

        for(ShareIngredientRequest s:shareIngredientRequests){ // 나눔식재료 하나하나 post 등록
            s.setSharePost(post);
            ShareIngredient shareIngredient=s.toEntity();
            shareBoardUpdateRequest.getShareIngredients().add(shareIngredient);
        }

        post.update(shareBoardUpdateRequest);
        return post;
    }

    @Override
    @Transactional
    public void delete(Long shareBoardId) {
        shareBoardRepository.deleteById(shareBoardId);
    }
}
