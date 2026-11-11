package com.bookmuse.app.security;

import com.bookmuse.app.models.Club;
import com.bookmuse.app.models.Comment;
import com.bookmuse.app.models.Discussion;
import com.bookmuse.app.models.Follow;
import com.bookmuse.app.models.LibraryEntry;
import com.bookmuse.app.models.Membership;
import com.bookmuse.app.models.Notification;
import com.bookmuse.app.models.Post;
import com.bookmuse.app.models.PostLike;
import com.bookmuse.app.models.Reply;
import com.bookmuse.app.models.Share;
import com.bookmuse.app.models.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Read visibility for the authenticated caller; unauthenticated startup/authentication reads are internal. */
public final class AdministratorVisibility {
    private AdministratorVisibility() {}

    public static boolean restrictsCurrentViewer() {
        Authentication authentication=SecurityContextHolder.getContext().getAuthentication();
        if(authentication==null) return false;
        return !authentication.isAuthenticated()
            || authentication.getAuthorities().stream().noneMatch(a->"ROLE_ADMIN".equals(a.getAuthority()));
    }

    /** Internal entity/field names only. This predicate contains no request-supplied SQL. */
    public static String predicate(Class<?> type) {
        if(!restrictsCurrentViewer()) return "";
        if(type==User.class) return "e.role<>'ADMIN'";
        if(type==Post.class || type==LibraryEntry.class) return user("e.userId");
        if(type==Club.class) return user("e.creatorId");
        if(type==Comment.class) return user("e.userId")+" and "+post("e.postId");
        if(type==Discussion.class) return user("e.userId")+" and "+club("e.clubId");
        if(type==Reply.class) return user("e.userId")+" and "+discussion("e.discussionId");
        if(type==Membership.class) return user("e.userId")+" and "+club("e.clubId");
        if(type==PostLike.class || type==Share.class) return user("e.userId")+" and "+post("e.postId");
        if(type==Follow.class) return user("e.followerId")+" and "+user("e.followingId");
        if(type==Notification.class) {
            // Retain historical notifications for deleted resources, but hide existing admin-owned targets.
            return user("e.actorId")
                +" and (e.type<>'NEW_FOLLOWER' or "+user("e.resourceId")+")"
                +" and (e.type not in ('LIKE','SHARE','COMMENT') or "+post("e.resourceId")
                +" or not exists (select vp.id from Post vp where vp.id=e.resourceId))"
                +" and (e.type<>'CLUB_JOIN' or "+club("e.resourceId")
                +" or not exists (select vc.id from Club vc where vc.id=e.resourceId))"
                +" and (e.type<>'DISCUSSION_REPLY' or "+discussion("e.resourceId")
                +" or not exists (select vd.id from Discussion vd where vd.id=e.resourceId))";
        }
        return "";
    }

    private static String user(String field) {
        return field+" in (select visibleUser.id from User visibleUser where visibleUser.role<>'ADMIN')";
    }
    private static String post(String field) {
        return field+" in (select visiblePost.id from Post visiblePost where "+user("visiblePost.userId")+")";
    }
    private static String club(String field) {
        return field+" in (select visibleClub.id from Club visibleClub where "+user("visibleClub.creatorId")+")";
    }
    private static String discussion(String field) {
        return field+" in (select visibleDiscussion.id from Discussion visibleDiscussion where "
            +user("visibleDiscussion.userId")+" and "+club("visibleDiscussion.clubId")+")";
    }
}
