import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';

// Layout
import DashboardLayout from '../components/layout/DashboardLayout';
import ProtectedRoute from './ProtectedRoute';

// Auth Pages
import Login from '../pages/auth/Login';
import Register from '../pages/auth/Register';
import PendingApproval from '../pages/auth/PendingApproval';
import RoleSelection from '../pages/auth/RoleSelection';
import ActivateAccount from '../pages/auth/ActivateAccount';
import Onboarding from '../pages/auth/Onboarding';

// Student Pages
import StudentDashboard from '../pages/student/StudentDashboard';

// Team Pages
import TeamDashboard from '../pages/team/TeamDashboard';
import TrackTopic from '../pages/team/TrackTopic';
import CreateTeam from '../pages/team/CreateTeam';
import JoinTeam from '../pages/team/JoinTeam';
import JoinRequests from '../pages/team/JoinRequests';
import MyTeam from '../pages/team/MyTeam';
import TeamMembers from '../pages/team/TeamMembers';
import SubmissionManagement from '../pages/team/SubmissionManagement';
import SubmissionHistory from '../pages/team/SubmissionHistory';
import NoticeBoard from '../pages/team/NoticeBoard';
import DeadlinesSchedule from '../pages/team/DeadlinesSchedule';
import Profile from '../pages/team/Profile';
import TeamJourney from '../pages/team/TeamJourney';
import TeamScoreDetails from '../pages/team/TeamScoreDetails';
import TeamChat from '../pages/team/TeamChat';
import CaseForm from '../pages/team/CaseForm';
import CaseList from '../pages/team/CaseList';
import PreviousTeamList from '../pages/team/PreviousTeamList';
import TeamReactivationForm from '../pages/team/TeamReactivationForm';
import CaseManagement from '../pages/coordinator/CaseManagement';

// Mentor Pages
import MentorDashboard from '../pages/mentor/MentorDashboard';
import AssignedCategories from '../pages/mentor/AssignedCategories';
import AssignedTeams from '../pages/mentor/AssignedTeams';
import TeamDetails from '../pages/mentor/TeamDetails';
import SubmissionReview from '../pages/mentor/SubmissionReview';
import FeedbackCenter from '../pages/mentor/FeedbackCenter';
import MentorNoticeBoard from '../pages/mentor/MentorNoticeBoard';
import MentorProfile from '../pages/mentor/MentorProfile';
import CreateMentorIncidentReport from '../pages/mentor/CreateIncidentReport';

import JudgeDashboard from '../pages/judge/JudgeDashboard';
import AssignedSubmissions from '../pages/judge/AssignedSubmissions';
import ScoringInterface from '../pages/judge/ScoringInterface';
import ViewEvaluation from '../pages/judge/ViewEvaluation';
import JudgeNoticeBoard from '../pages/judge/JudgeNoticeBoard';
import JudgeProfile from '../pages/judge/JudgeProfile';
import CreateJudgeIncidentReport from '../pages/judge/CreateIncidentReport';

// Coordinator Pages
import CoordinatorDashboard from '../pages/coordinator/CoordinatorDashboard';
import EventManagement from '../pages/coordinator/EventManagement';
import EventDetails from '../pages/coordinator/EventDetails';
import TeamManagement from '../pages/coordinator/TeamManagement';
import TeamDetail from '../pages/coordinator/TeamDetail';
import RecordTeam from '../pages/coordinator/RecordTeam';
import SubmissionManagementCoordinator from '../pages/coordinator/SubmissionManagement';
import UserApproval from '../pages/coordinator/UserApproval';
import CriteriaManagement from '../pages/coordinator/CriteriaManagement';
import RankingManagement from '../pages/coordinator/RankingManagement';
import RankingDetail from '../pages/coordinator/RankingDetail';
import ScoringAnalytics from '../pages/coordinator/ScoringAnalytics';
import AwardsManagement from '../pages/coordinator/AwardsManagement';
import AwardForm from '../pages/coordinator/AwardForm';
import Reports from '../pages/coordinator/Reports';
import AuditLogs from '../pages/coordinator/AuditLogs';
import CoordinatorProfile from '../pages/coordinator/CoordinatorProfile';
import SubmissionDetail from '../pages/coordinator/SubmissionDetail';
import EventTimeline from '../pages/coordinator/EventTimeline';
import AppealsInbox from '../pages/coordinator/AppealsInbox';
import StaffManagement from '../pages/coordinator/StaffManagement';
import SeedingManagement from '../pages/coordinator/SeedingManagement';

const AppRoutes = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/pending-approval" element={<PendingApproval />} />
        <Route path="/select-role" element={<ProtectedRoute allowedRoles={['coordinator', 'judge', 'mentor', 'team_leader', 'team_member']}><RoleSelection /></ProtectedRoute>} />
        <Route path="/activate-account" element={<ActivateAccount />} />
        <Route path="/onboarding" element={<ProtectedRoute allowedRoles={['coordinator', 'judge', 'mentor', 'team_leader', 'team_member']}><Onboarding /></ProtectedRoute>} />
        
        {/* Student Routes */}
        <Route path="/student" element={
          <ProtectedRoute allowedRoles={['team_leader', 'team_member']}>
            <DashboardLayout role="student" />
          </ProtectedRoute>
        }>
          <Route index element={<Navigate to="dashboard" replace />} />
          <Route path="dashboard" element={<StudentDashboard />} />
          <Route path="create-team" element={<CreateTeam />} />
          <Route path="join-team" element={<JoinTeam />} />
          <Route path="previous-teams" element={<PreviousTeamList />} />
          <Route path="previous-teams/:profileId/reactivate" element={<TeamReactivationForm />} />
          <Route path="profile" element={<Profile />} />
        </Route>

        {/* Team Routes */}
        <Route path="/team" element={
          <ProtectedRoute allowedRoles={['team_leader', 'team_member']}>
            <DashboardLayout role="team" />
          </ProtectedRoute>
        }>
          <Route index element={<Navigate to="dashboard" replace />} />
          <Route path="dashboard" element={<TeamDashboard />} />
          <Route path="topic" element={<TrackTopic />} />
          <Route path="my-team" element={<MyTeam />} />
          <Route path="members" element={<TeamMembers />} />
          <Route path="join-requests" element={<JoinRequests />} />
          <Route path="chat" element={<TeamChat />} />
          <Route path="submissions" element={<SubmissionManagement />} />
          <Route path="history" element={<SubmissionHistory />} />
          <Route path="history/:id" element={<TeamScoreDetails />} />
          <Route path="journey" element={<TeamJourney />} />
          <Route path="notices" element={<NoticeBoard />} />
          <Route path="schedule" element={<DeadlinesSchedule />} />
          <Route path="support" element={<Navigate to="/team/cases" replace />} />
          <Route path="cases" element={<CaseList />} />
          <Route path="cases/new" element={<CaseForm />} />
          <Route path="previous-teams" element={<PreviousTeamList />} />
          <Route path="previous-teams/:profileId/reactivate" element={<TeamReactivationForm />} />
          <Route path="profile" element={<Profile />} />
        </Route>

        {/* Mentor Routes */}
        <Route path="/mentor" element={
          <ProtectedRoute allowedRoles={['mentor']}>
            <DashboardLayout role="mentor" />
          </ProtectedRoute>
        }>
          <Route index element={<Navigate to="dashboard" replace />} />
          <Route path="dashboard" element={<MentorDashboard />} />
          <Route path="categories" element={<AssignedCategories />} />
          <Route path="teams" element={<AssignedTeams />} />
          <Route path="team-details" element={<TeamDetails />} />
          <Route path="review" element={<SubmissionReview />} />
          <Route path="feedback" element={<FeedbackCenter />} />
          <Route path="notices" element={<MentorNoticeBoard />} />
          <Route path="incidents/create" element={<CreateMentorIncidentReport />} />
          <Route path="profile" element={<MentorProfile />} />
        </Route>

        {/* Judge Routes */}
        <Route path="/judge" element={
          <ProtectedRoute allowedRoles={['judge']}>
            <DashboardLayout role="judge" />
          </ProtectedRoute>
        }>
          <Route index element={<Navigate to="dashboard" replace />} />
          <Route path="dashboard" element={<JudgeDashboard />} />
          <Route path="submissions" element={<AssignedSubmissions />} />
          <Route path="score/:id" element={<ScoringInterface />} />
          <Route path="view-evaluation/:id" element={<ViewEvaluation />} />
          <Route path="notices" element={<JudgeNoticeBoard />} />
          <Route path="incidents/create" element={<CreateJudgeIncidentReport />} />
          <Route path="profile" element={<JudgeProfile />} />
        </Route>

        {/* Coordinator Routes */}
        <Route path="/coordinator" element={
          <ProtectedRoute allowedRoles={['coordinator']}>
            <DashboardLayout role="coordinator" />
          </ProtectedRoute>
        }>
          <Route index element={<Navigate to="dashboard" replace />} />
          <Route path="dashboard" element={<CoordinatorDashboard />} />
          <Route path="events" element={<EventManagement />} />
          <Route path="events/:id" element={<EventDetails />} />
          <Route path="teams" element={<TeamManagement />} />
          <Route path="teams/:id" element={<TeamDetail />} />
          <Route path="teams/record" element={<RecordTeam />} />
          <Route path="submissions" element={<SubmissionManagementCoordinator />} />
          <Route path="submissions/:id" element={<SubmissionDetail />} />
          <Route path="staff" element={<StaffManagement />} />
          <Route path="mentors/*" element={<Navigate to="/coordinator/staff" replace />} />
          <Route path="judges/*" element={<Navigate to="/coordinator/staff" replace />} />
          <Route path="users" element={<UserApproval />} />
          <Route path="criteria" element={<CriteriaManagement />} />
          <Route path="ranking" element={<RankingManagement />} />
          <Route path="ranking/:id" element={<RankingDetail />} />
          <Route path="scoring" element={<ScoringAnalytics />} />
          <Route path="awards" element={<AwardsManagement />} />
          <Route path="awards/new" element={<AwardForm />} />
          <Route path="awards/:id/edit" element={<AwardForm />} />
          <Route path="incidents/*" element={<Navigate to="/coordinator/cases" replace />} />
          <Route path="reports" element={<Reports />} />
          <Route path="timeline" element={<EventTimeline />} />
          <Route path="appeals" element={<AppealsInbox />} />
          <Route path="seeding" element={<SeedingManagement />} />
          <Route path="logs" element={<AuditLogs />} />
          <Route path="support" element={<Navigate to="/coordinator/cases" replace />} />
          <Route path="cases" element={<CaseManagement />} />
          <Route path="profile" element={<CoordinatorProfile />} />
        </Route>

        {/* Fallback */}
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
};

export default AppRoutes;
