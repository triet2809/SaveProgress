export const users = {
  team: {
    name: 'Alex Chen',
    email: 'alex.chen@fpt.edu.vn',
    role: 'Team Leader',
    teamName: 'Neural Nexus',
    initials: 'AC',
    universityId: 'FPT-2026-0472',
    department: 'Computer Science',
    yearOfStudy: '4th Year Undergraduate',
    phone: '+84 123 456 789',
    teamRole: 'Team Lead'
  },
  student: {
    name: 'Emily Davis',
    email: 'emily.d@fpt.edu.vn',
    role: 'Student',
    teamName: 'None',
    initials: 'ED',
    universityId: 'FPT-2026-0899',
    department: 'Software Engineering',
    yearOfStudy: '2nd Year Undergraduate',
    phone: '+84 111 222 333',
    teamRole: 'Participant'
  },
  mentor: {
    name: 'Dr. Priya Patel',
    email: 'p.patel@fpt.edu.vn',
    role: 'Mentor',
    initials: 'PP',
    universityId: 'FPT-FAC-1024',
    department: 'Computer Science',
    position: 'Faculty',
    phone: '+84 987 654 321',
    specialty: 'Faculty Mentor · AI/ML'
  },
  judge: {
    name: 'Prof. James Kim',
    email: 'j.kim@meridian.edu',
    role: 'Judge',
    initials: 'JK',
    universityId: 'MU-2026-0472',
    department: 'Computer Science',
    position: 'Professor',
    phone: '+1 (555) 214-8830',
    specialty: 'Judge · AI/ML Track'
  },
  coordinator: {
    name: 'Sarah Connor',
    email: 's.connor@fpt.edu.vn',
    role: 'Coordinator',
    initials: 'SC',
    universityId: 'FPT-ADMIN-101',
    department: 'Events',
    position: 'Lead Event Coordinator',
    phone: '+84 111 222 333',
    specialty: 'Admin'
  }
};

export const eventDetails = {
  name: 'SEAL Hackathon 2026',
  date: 'June 20–22',
  location: 'Engineering Complex',
  daysRemaining: 4,
  organizer: 'FPT University'
};

export const teamData = {
  teamName: 'Neural Nexus',
  project: 'EduTrack AI',
  description: 'Intelligent Learning Analytics Platform',
  fullDescription: 'EduTrack AI uses machine learning to analyze student learning patterns and provide personalized recommendations to educators and students. The platform integrates with existing LMS systems and provides real-time dashboards for academic performance tracking.',
  progress: 65,
  category: 'AI/ML',
  memberCount: 4,
  status: 'Draft',
  techStack: ['AI/ML', 'Python', 'React', 'TensorFlow', 'FastAPI'],
  resources: [
    { id: 1, name: 'GitHub Repository', type: 'github', url: '#' },
    { id: 2, name: 'Live Demo', type: 'globe', url: '#' },
    { id: 3, name: 'Project Docs', type: 'file', url: '#' },
    { id: 4, name: 'Design Files', type: 'external', url: '#' }
  ],
  mentor: {
    name: 'Dr. Priya Patel',
    role: 'AI/ML Faculty',
    initials: 'PP'
  }
};

export const notifications = [
  {
    id: 1,
    message: 'Dr. Patel reviewed your draft submission',
    time: '2h ago',
    type: 'info'
  },
  {
    id: 2,
    message: 'New workshop: AI Ethics in Practice (June 17)',
    time: '5h ago',
    type: 'event'
  },
  {
    id: 3,
    message: 'Maya Rodriguez joined your team workspace',
    time: '1d ago',
    type: 'success'
  }
];

export const mentorTeams = [
  { id: 1, name: 'Neural Nexus', project: 'EduTrack AI', complete: 80, status: 'On Track', color: 'success' },
  { id: 2, name: 'ByteBuilders', project: 'SupplyChain Vista', complete: 60, status: 'Needs Attention', color: 'warning' },
  { id: 3, name: 'CodeCraft', project: 'MediConnect Pro', complete: 45, status: 'At Risk', color: 'danger' }
];

export const judgeSubmissions = [
  { id: 1, team: 'Neural Nexus', project: 'EduTrack AI', category: 'AI/ML', priority: 'High', status: 'Pending', initials: 'N' },
  { id: 2, team: 'DataForge', project: 'Predictive Analytics', category: 'Data Science', priority: 'High', status: 'Pending', initials: 'D' },
  { id: 3, team: 'MLMasters', project: 'NeuralVision CV', category: 'AI/ML', priority: 'Medium', status: 'Pending', initials: 'M' },
  { id: 4, team: 'QuantumLeap', project: 'TensorFlow Flow', category: 'AI/ML', priority: 'Medium', status: 'Pending', initials: 'Q' },
  { id: 5, team: 'ByteWave', project: 'DataStream Pro', category: 'Data Science', priority: 'Low', status: 'Pending', initials: 'B' }
];

export const teamMembersList = [
  { id: 1, initials: 'AC', name: 'Alex Chen', role: 'Team Lead', email: 'alex.chen@fpt.edu.vn', skills: ['ML Engineering', 'Python'] },
  { id: 2, initials: 'MR', name: 'Maya Rodriguez', role: 'Backend Developer', email: 'm.rodriguez@fpt.edu.vn', skills: ['FastAPI', 'PostgreSQL'] },
  { id: 3, initials: 'JK', name: 'Jordan Kim', role: 'ML Engineer', email: 'j.kim@fpt.edu.vn', skills: ['TensorFlow', 'PyTorch'] },
  { id: 4, initials: 'ST', name: 'Sam Taylor', role: 'UI/UX Designer', email: 's.taylor@fpt.edu.vn', skills: ['React', 'Figma'] }
];

export const submissionHistoryList = [
  { id: 1, version: 'v0.2', title: 'EduTrack AI — Feature Complete', date: 'June 14, 2026', size: '28.4 MB', status: 'Previous' },
  { id: 2, version: 'v0.1', title: 'EduTrack AI — Initial Prototype', date: 'June 10, 2026', size: '12.1 MB', status: 'Previous' }
];

export const noticesList = [
  { id: 1, authorName: 'Dr. Sarah Jenkins', authorRole: 'Judge', authorInitials: 'SJ', date: 'June 17, 2026 - 09:00 AM', title: 'Evaluation Rubrics Updated', content: 'Please review the updated evaluation rubrics for the AI/ML category. We have added a new section prioritizing ethical data usage.', priority: 'High' },
  { id: 2, authorName: 'Marcus Wright', authorRole: 'Mentor', authorInitials: 'MW', date: 'June 16, 2026 - 14:30 PM', title: 'Mentorship Session Availability', content: 'I have opened up 3 new slots for 1-on-1 architecture reviews tomorrow afternoon. Book them via the scheduling portal.', priority: 'Normal' },
  { id: 3, authorName: 'Hackathon Admin', authorRole: 'Admin', authorInitials: 'AD', date: 'June 15, 2026 - 08:00 AM', title: 'Welcome to SEAL Hackathon 2026', content: 'Welcome teams! Please ensure your project repository links are correctly submitted under the Submission Management tab by tomorrow.', priority: 'High' }
];

export const scheduleList = [
  { id: 1, date: 'June 16', day: 'Today', badge: 'Today', type: 'active', events: ['Team Registration Closes (11:59 PM)', 'Mentor Assignment Notifications Sent'] },
  { id: 2, date: 'June 17', day: 'Monday', type: 'default', events: ['Workshop: AI Ethics in Practice (2:00 PM)', 'GitHub Repo Verification (5:00 PM)'] },
  { id: 3, date: 'June 18', day: 'Tuesday', type: 'default', events: ['Mentor Office Hours (10:00 AM - 4:00 PM)', 'Practice Pitch Sessions (8:00 PM)'] },
  { id: 4, date: 'June 19', day: 'Wednesday', badge: 'Deadline', type: 'danger', events: ['Final Submission Deadline — 11:59 PM', 'Late submissions not accepted'] },
  { id: 5, date: 'June 20', day: 'Thursday', type: 'default', events: ['Hackathon Opens — 9:00 AM', 'Opening Ceremony — 10:00 AM'] },
  { id: 6, date: 'June 21', day: 'Friday', type: 'default', events: ['Full-day Hacking Session', 'Mid-event Check-in — 12:00 PM'] },
  { id: 7, date: 'June 22', day: 'Saturday', badge: 'Awards', type: 'success', events: ['Project Demo Day — 10:00 AM to 3:00 PM', 'Awards Ceremony — 5:00 PM'] }
];

export const mentorCategories = [
  { id: 1, name: 'AI & Machine Learning', icon: 'Shield', description: 'Machine learning, deep learning, NLP, computer vision, and AI-driven solutions', totalTeams: 12, submittedTeams: 8, color: 'blue' },
  { id: 2, name: 'Data Science', icon: 'Tag', description: 'Data analytics, visualization, statistical modeling, and predictive analytics', totalTeams: 8, submittedTeams: 5, color: 'purple' }
];

export const mentorAssignedTeams = [
  { id: 1, name: 'Neural Nexus', initials: 'N', project: 'EduTrack AI', members: 4, category: 'AI/ML', progress: 80, lastActive: '2h ago', status: 'On Track' },
  { id: 2, name: 'ByteBuilders', initials: 'B', project: 'SupplyChain Vista', members: 3, category: 'Web Dev', progress: 60, lastActive: '5h ago', status: 'Needs Attention' },
  { id: 3, name: 'CodeCraft', initials: 'C', project: 'MediConnect Pro', members: 4, category: 'AI/ML', progress: 45, lastActive: '1d ago', status: 'At Risk' }
];

export const mentorTeamDetails = [
  {
    id: 1,
    teamInfo: { name: 'Neural Nexus', category: 'AI & Machine Learning' },
    project: {
      title: 'EduTrack AI',
      subtitle: 'Intelligent Learning Analytics Platform',
      description: 'EduTrack AI uses machine learning to analyze student learning patterns and provide personalized recommendations. Integrates with LMS systems for real-time analytics.',
      tags: ['AI/ML', 'Python', 'TensorFlow', 'React'],
      progress: 80,
      status: 'On Track'
    },
    roster: [
      { id: 1, initials: 'AC', name: 'Alex Chen', role: 'Team Lead' },
      { id: 2, initials: 'MR', name: 'Maya Rodriguez', role: 'Backend' },
      { id: 3, initials: 'JK', name: 'Jordan Kim', role: 'ML Engineer' },
      { id: 4, initials: 'ST', name: 'Sam Taylor', role: 'UI/UX' }
    ]
  },
  {
    id: 2,
    teamInfo: { name: 'ByteBuilders', category: 'Web Development' },
    project: {
      title: 'SupplyChain Vista',
      subtitle: 'Blockchain-based Logistics Tracking Platform',
      description: 'SupplyChain Vista provides transparent, immutable tracking of goods from manufacturer to consumer. Utilizes smart contracts to automate payments upon delivery confirmation.',
      tags: ['Web3', 'Solidity', 'Node.js', 'Next.js'],
      progress: 60,
      status: 'Needs Attention'
    },
    roster: [
      { id: 5, initials: 'LJ', name: 'Liam Johnson', role: 'Full Stack' },
      { id: 6, initials: 'ES', name: 'Emma Smith', role: 'Smart Contracts' },
      { id: 7, initials: 'DB', name: 'David Brown', role: 'Frontend' }
    ]
  },
  {
    id: 3,
    teamInfo: { name: 'CodeCraft', category: 'AI & Machine Learning' },
    project: {
      title: 'MediConnect Pro',
      subtitle: 'AI-Driven Patient Triaging System',
      description: 'MediConnect Pro utilizes natural language processing to pre-screen patient symptoms via a chatbot, directing them to the appropriate care level and reducing emergency room bottlenecks.',
      tags: ['NLP', 'Python', 'PyTorch', 'Vue.js'],
      progress: 45,
      status: 'At Risk'
    },
    roster: [
      { id: 8, initials: 'OC', name: 'Olivia Carter', role: 'AI Researcher' },
      { id: 9, initials: 'MW', name: 'Marcus White', role: 'Data Engineer' },
      { id: 10, initials: 'SP', name: 'Sophia Patel', role: 'Backend API' },
      { id: 11, initials: 'ED', name: 'Ethan Davis', role: 'UI/UX' }
    ]
  }
];

export const mentorSubmissions = [
  { id: 1, teamName: 'Neural Nexus', projectName: 'EduTrack AI', submittedDate: 'June 14', version: 'v0.2', status: 'Pending Review', round: 'Preliminary'},
  { id: 2, teamName: 'ByteBuilders', projectName: 'SupplyChain Vista', submittedDate: 'June 13', version: 'v0.1', status: 'Reviewed', round: 'Preliminary' },
  { id: 3, teamName: 'CodeCraft', projectName: 'MediConnect Pro', submittedDate: 'June 15', version: 'v0.1', status: 'Pending Review', round: 'Final' }
];

export const mentorFeedbackHistory = {
  1: [ // Neural Nexus
    { id: 1, author: 'Dr. Priya Patel', date: 'June 15', message: 'Great progress on the ML model architecture. The attention mechanism you described is promising. Consider adding cross-validation to your training pipeline for more robust results.' },
    { id: 2, author: 'Dr. Priya Patel', date: 'June 12', message: 'Your initial prototype is solid. Focus on improving the data preprocessing pipeline — the current implementation may not scale well to larger datasets. Check out scikit-learn pipelines.' }
  ],
  2: [ // ByteBuilders
    { id: 3, author: 'Dr. Priya Patel', date: 'June 13', message: 'Excellent work setting up the smart contracts. Make sure to run comprehensive security audits before final submission, particularly around the payment gateway logic.' }
  ],
  3: [ // CodeCraft
    { id: 4, author: 'Dr. Priya Patel', date: 'June 10', message: 'The NLP approach to triaging is innovative. I suggest refining the training dataset to reduce bias, and ensure the chatbot UI is highly accessible for all patient demographics.' },
    { id: 5, author: 'Dr. Priya Patel', date: 'June 08', message: 'Good initial pitch. Please flesh out the user journey map for the first prototype.' }
  ]
};

export const judgeAssignedSubmissions = [
  {
    id: 1,
    teamName: 'Neural Nexus',
    initials: 'NN',
    project: 'EduTrack AI',
    category: 'AI/ML',
    track: 'AI/ML Track A',
    round: 'Finals',
    submitted: 'June 18, 2026',
    status: 'Pending',
    score: null,
  },
  {
    id: 2,
    teamName: 'DataCraft',
    initials: 'DC',
    project: 'Predictive Analytics Engine',
    category: 'Data Science',
    track: 'Data Track B',
    round: 'Finals',
    submitted: 'June 17, 2026',
    status: 'Completed',
    score: 85,
  },
  {
    id: 3,
    teamName: 'ByteBuilders',
    initials: 'BB',
    project: 'Campus Connect',
    category: 'Web Dev',
    track: 'Web Track A',
    round: 'Finals',
    submitted: 'June 16, 2026',
    status: 'Completed',
    score: 92,
  },
];

export const judgeSubmissionDetails = {
  teamName: 'Neural Nexus',
  project: 'EduTrack AI',
  description: 'EduTrack AI is a personalized learning platform that uses machine learning to adapt educational content to the pacing and learning style of individual students. By analyzing quiz results, engagement metrics, and reading speed, the platform dynamically generates custom lesson plans.',
  techStack: ['Python', 'TensorFlow', 'React', 'Node.js', 'PostgreSQL'],
  roster: [
    { name: 'Alex Chen', role: 'Team Lead & ML Engineer' },
    { name: 'Sarah Jenkins', role: 'Frontend Developer' },
    { name: 'Marcus Johnson', role: 'Backend Developer' },
    { name: 'Emily Davis', role: 'UI/UX Designer' }
  ],
  files: [
    { id: 1, name: 'PitchDeck_Final.pdf', size: '2.4 MB', type: 'PDF' },
    { id: 2, name: 'Architecture_Diagram.png', size: '1.1 MB', type: 'Image' },
    { id: 3, name: 'Source_Code_v1.zip', size: '14.5 MB', type: 'Archive' },
    { id: 4, name: 'Demo_Recording.mp4', size: '45.2 MB', type: 'Video' }
  ]
};

export const incidentsList = [
  { id: 'IR-2026-001', team: 'Neural Nexus', reporter: 'Dr. Priya Patel', role: 'Mentor', type: 'Code Plagiarism', severity: 'High', status: 'Pending Review', date: 'June 18, 2026' },
  { id: 'IR-2026-002', team: 'ByteBuilders', reporter: 'Prof. James Kim', role: 'Judge', type: 'Late Submission Bypass', severity: 'Medium', status: 'Under Review', date: 'June 17, 2026' },
  { id: 'IR-2026-003', team: 'DataCraft', reporter: 'Michael Ross', role: 'Guest Judge', type: 'Inappropriate Content', severity: 'High', status: 'Warning Issued', date: 'June 16, 2026' },
  { id: 'IR-2026-004', team: 'AlgoArts', reporter: 'Sarah Connor', role: 'Coordinator', type: 'Team Size Limit Exceeded', severity: 'Low', status: 'Resolved', date: 'June 15, 2026' }
];

export const incidentDetail = {
  id: 'IR-2026-001',
  event: 'SEAL Hackathon 2026',
  round: 'Preliminary Submission',
  category: 'AI/ML',
  team: 'Neural Nexus',
  submission: 'EduTrack AI v0.2',
  incidentType: 'Code Plagiarism',
  severity: 'High',
  status: 'Pending Review',
  title: 'Suspected use of pre-existing proprietary codebase',
  description: 'Upon reviewing the submitted architecture and core inference engine for EduTrack AI, I noticed significant portions of the code match a proprietary repository owned by EduTech Corp. The implementation details, including specific variable naming conventions and custom algorithm structures, are identical. This violates the rule stating all core code must be written during the hackathon or open-source.',
  evidenceUrl: 'https://github.com/mock-evidence/plagiarism-report',
  reporter: {
    name: 'Dr. Priya Patel',
    role: 'Mentor',
    email: 'p.patel@fpt.edu.vn',
    date: 'June 18, 2026 - 14:30 PM'
  },
  auditLogs: [
    { date: 'June 18, 2026 - 14:30 PM', action: 'Report Submitted', user: 'Dr. Priya Patel' }
  ]
};

export const trackTopicDetails = {
  theme: "AI for Social Good",
  track: "AI/ML",
  description: "Leverage Artificial Intelligence and Machine Learning to build solutions that address pressing social, environmental, or educational challenges. Your project should demonstrate a clear positive impact on the community.",
  requirements: [
    "Core logic must utilize an AI/ML model (e.g., NLP, Computer Vision, Predictive Analytics).",
    "Model training or fine-tuning must be documented.",
    "Must include a working prototype (web or mobile interface) to interact with the model.",
    "Source code must be submitted via a public or private GitHub repository.",
    "A 3-minute video demonstration is required for the final submission."
  ],
  evaluationCriteria: [
    { name: "Innovation", weight: "25%", description: "Originality of the idea and approach to solving the problem." },
    { name: "Technical Execution", weight: "35%", description: "Quality of the code, model accuracy, and robustness of the implementation." },
    { name: "UI/UX", weight: "20%", description: "Ease of use, accessibility, and visual design of the prototype." },
    { name: "Impact & Practicality", weight: "20%", description: "Real-world applicability and potential social impact of the solution." }
  ]
};
