package vn.edu.fpt.seal.modules.prize.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.seal.common.enums.TeamStatus;
import vn.edu.fpt.seal.common.exception.ApiException;
import vn.edu.fpt.seal.modules.event.entity.Event;
import vn.edu.fpt.seal.modules.event.repository.EventRepository;
import vn.edu.fpt.seal.modules.prize.dto.*;
import vn.edu.fpt.seal.modules.prize.entity.Prize;
import vn.edu.fpt.seal.modules.prize.mapper.PrizeMapper;
import vn.edu.fpt.seal.modules.prize.repository.PrizeRepository;
import vn.edu.fpt.seal.modules.team.entity.Team;
import vn.edu.fpt.seal.modules.team.repository.TeamRepository;
import vn.edu.fpt.seal.modules.timeline.*;
import vn.edu.fpt.seal.modules.timeline.dto.TimelineEventRequest;
import vn.edu.fpt.seal.modules.timeline.service.TimelineService;
import vn.edu.fpt.seal.modules.track.entity.Track;
import vn.edu.fpt.seal.modules.track.repository.TrackRepository;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class PrizeService {
    private final PrizeRepository repo;
    private final EventRepository eventRepo;
    private final TrackRepository trackRepo;
    private final TeamRepository teamRepo;
    private final vn.edu.fpt.seal.modules.round.service.CompetitionLifecycleService lifecycleService;
    private final vn.edu.fpt.seal.modules.seeding.service.SeedingService seedingService;
    @Autowired private TimelineService timeline;

    @Transactional(readOnly=true)
    public Page<PrizeResponse> list(UUID eventId, UUID trackId, UUID teamId, Pageable p) {
        Page<Prize> page=eventId!=null?repo.findByEventId(eventId,p):trackId!=null?repo.findByTrackId(trackId,p):teamId!=null?repo.findByTeamId(teamId,p):repo.findAll(p);
        return page.map(PrizeMapper::toResponse);
    }
    @Transactional(readOnly=true) public PrizeResponse get(UUID id){return PrizeMapper.toResponse(find(id));}

    @Transactional
    public PrizeResponse create(CreatePrizeRequest r) {
        Event e=eventRepo.findById(r.eventId()).orElseThrow(()->ApiException.notFound("Event not found: "+r.eventId()));
        lifecycleService.requireAwardsAllowed(e.getId()); seedingService.requireEventFinalized(e.getId());
        Track tr=track(r.trackId()); Team tm=team(r.teamId());
        if (tm != null && tm.getStatus() == TeamStatus.disqualified) {
            throw ApiException.badRequest("Cannot award a prize to a disqualified team");
        }
        validateScope(e,tr,tm);
        Prize prize=repo.save(Prize.builder().event(e).track(tr).team(tm).name(r.name().trim())
                .prizeAmount(r.prizeAmount()).description(trim(r.description())).awardedAt(r.awardedAt()).build());
        record(prize, TimelineEventType.PRIZE_CONFIGURED);
        return PrizeMapper.toResponse(prize);
    }
    @Transactional
    public PrizeResponse update(UUID id,UpdatePrizeRequest r) {
        Prize p=find(id); lifecycleService.requireAwardsAllowed(p.getEvent().getId()); seedingService.requireEventFinalized(p.getEvent().getId());
        if(r.trackId()!=null)p.setTrack(track(r.trackId())); if(r.teamId()!=null)p.setTeam(team(r.teamId()));
        validateScope(p.getEvent(),p.getTrack(),p.getTeam()); if(r.name()!=null)p.setName(r.name().trim());
        if(r.prizeAmount()!=null)p.setPrizeAmount(r.prizeAmount()); if(r.description()!=null)p.setDescription(trim(r.description()));
        if(r.awardedAt()!=null)p.setAwardedAt(r.awardedAt());
        // Policy: PRIZE_CONFIGURED is the immutable initial-configuration milestone.
        // Later edits intentionally create no timeline entry; this avoids claiming
        // a new milestone without a persisted configuration version or hash.
        return PrizeMapper.toResponse(p);
    }
    @Transactional public void delete(UUID id){Prize p=find(id);lifecycleService.requireAwardsAllowed(p.getEvent().getId());seedingService.requireEventFinalized(p.getEvent().getId());repo.delete(p);}

    private void record(Prize p, TimelineEventType type) {
        if (timeline == null) return;
        timeline.record(new TimelineEventRequest(p.getEvent().getId(), p.getTeam()==null?null:p.getTeam().getId(),
                null,p.getTrack()==null?null:p.getTrack().getId(),type,
                TimelineSourceType.PRIZE,p.getId(),TimelineScope.COORDINATOR_PRIVATE,
                "Prize configured", "A coordinator configured a prize", null,
                "prize:"+p.getId()+":configured"));
    }
    private Prize find(UUID id){return repo.findWithRelationsById(id).orElseThrow(()->ApiException.notFound("Prize not found: "+id));}
    private Track track(UUID id){return id==null?null:trackRepo.findById(id).orElseThrow(()->ApiException.notFound("Track not found: "+id));}
    private Team team(UUID id){return id==null?null:teamRepo.findById(id).orElseThrow(()->ApiException.notFound("Team not found: "+id));}
    private void validateScope(Event e,Track tr,Team tm){if(tr!=null&&!tr.getEvent().getId().equals(e.getId()))throw ApiException.badRequest("Track must belong to prize event");if(tm!=null){if(tr!=null&&!tm.getTrack().getId().equals(tr.getId()))throw ApiException.badRequest("Team must belong to prize track");if(!tm.getTrack().getEvent().getId().equals(e.getId()))throw ApiException.badRequest("Team must belong to prize event");}}
    private String trim(String s){return s==null?null:s.trim();}
}
