package com.ssafy.chatroom.db.repository;

import com.ssafy.chatroom.db.entity.ChatRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity,Integer> {
    Optional<ChatRoomEntity> findChatRoomEntityBySharePostIdAndAndSenderMemberId(Integer sharePostId,Long senderMemberId);
}
