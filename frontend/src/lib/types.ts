export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
  timestamp: string
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export interface UserDto {
  id: number
  username: string
  email: string
  displayName: string
  roles: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: UserDto
}

export interface ProjectDto {
  id: number
  name: string
  description: string
  ownerId: number
  createdAt: string
  memberCount?: number
}

export interface ProjectMemberDto {
  id: number
  username: string
  role: string
}

export interface ProjectDashboardDto {
  projectId: number
  name: string
  fileCount: number
  classCount: number
  methodCount: number
  dependencyCount: number
  apiEndpointCount: number
  indexedRepositoryCount: number
  totalRepositoryCount: number
}

export interface RepositoryDto {
  id: number
  projectId: number
  name: string
  url: string | null
  branch: string | null
  provider: string
  status: string
  indexedFileCount: number
  lastIndexedAt: string | null
  createdAt: string
}

export interface IndexStatusDto {
  repositoryId: number
  indexJobId: number
  status: string
  phase: string | null
  progress: number
  error: string | null
  incremental: boolean
  startedAt: string | null
  finishedAt: string | null
  fileCount: number
  classCount: number
  methodCount: number
  chunkCount: number
}

export interface RepositoryFileDto {
  id: number
  projectId: number
  repositoryId: number
  path: string
  language: string
  size: number
}

export interface CodeClassDto {
  id: number
  projectId: number
  repositoryId: number
  fileId: number
  name: string
  fqName: string
  kind: string
  annotations: string | null
  modifiers: string | null
  parentClass: string | null
  interfaces: string | null
  startLine: number
  endLine: number
  filePath: string
}

export interface CodeMethodDto {
  id: number
  classId: number
  name: string
  returnType: string
  parameters: string
  modifiers: string
  annotations: string
  httpMethod: string | null
  httpPath: string | null
  startLine: number
  endLine: number
  body: string
}

export interface CodeFieldDto {
  id: number
  classId: number
  name: string
  type: string
  modifiers: string
}

export interface CodeDependencyDto {
  id: number
  sourceClassFq: string
  targetClassFq: string
  type: string
}

export interface WhereUsedDto {
  sourceClassFq: string
  sourceMethodName: string | null
  relationType: string
  lineNumber: number
  filePath: string
  projectId: number
}

export interface ArchitectureNode {
  id: number
  name: string
  fqName: string
  stereotype: string
  kind: string
  filePath: string
}

export interface ArchitectureEdge {
  source: number
  target: number
  type: string
}

export interface ArchitectureGraph {
  nodes: ArchitectureNode[]
  edges: ArchitectureEdge[]
  stereotypes: Record<string, number>
}

export interface SearchResultDto {
  matchType: string
  filePath: string
  className: string | null
  methodName: string | null
  kind: string
  startLine: number
  endLine: number
  snippet: string
  score: number
  repositoryId: number | null
}

export interface ConversationDto {
  id: number
  title: string
  projectId: number
  createdAt: string
}

export interface ChatMessageDto {
  id: number
  role: string
  content: string
  createdAt: string
}

export interface ChatSource {
  filePath: string
  className: string | null
  methodName: string | null
  startLine: number
  endLine: number
  snippet: string
  score: number
}

export interface ChatResponse {
  messageId: number
  answer: string
  conversationId: number
  sources: ChatSource[]
}

export interface GitCommitDto {
  id: string
  author: string
  email: string
  date: string
  shortMessage: string
  fullMessage: string
}

export interface FileChange {
  changeType: string
  oldPath: string
  newPath: string
  additions: number
  deletions: number
}

export interface GitDiffDto {
  commitId: string
  changes: FileChange[]
  diff: string
}

export interface BlameLine {
  commitId: string
  author: string
  date: string
  line: number
  content: string
}

export interface BugAnalysisResponse {
  analysisId: number
  errorMessage: string
  rootCause: string
  confidence: number
  filePath: string
  lineNumber: number
  explanation: string | null
  suggestedFix: string | null
}

export interface ReviewFinding {
  severity: string
  category: string
  filePath: string
  line: number
  message: string
  suggestion: string | null
}

export interface ReviewResponse {
  summary: string
  findings: ReviewFinding[]
}

export interface GeneratedTests {
  fileName: string
  code: string
}

export interface GeneratedDocument {
  fileName: string
  contentType: string
  content: string
}

export interface AgentResponse {
  prompt: string
  answer: string
  toolCalls: string[]
}

export interface ToolDefinition {
  name: string
  description: string
  parameters: string
}

export interface ToolExecutionDto {
  id: number
  projectId: number
  userId: number
  toolName: string
  inputJson: string
  outputJson: string
  createdAt: string
}

export interface GeneratedPatch {
  id: number
  projectId: number
  repositoryId: number
  instruction: string
  diff: string
  status: string
  createdAt: string
  appliedAt: string | null
}

export interface AuditLogDto {
  id: number
  userId: number
  projectId: number | null
  action: string
  entityRef: string | null
  detail: string | null
  createdAt: string
}

export interface MetricsSnapshot {
  startedAt: string
  aiRequests: number
  aiTokens: number
  toolCalls: number
  retrievals: number
  indexingRuns: number
  failedAiCalls: number
  latencies: Record<string, { count: number; totalMs: number }>
}