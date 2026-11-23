import { useEffect, useMemo, useState } from 'react'
import { createClient, type Session } from '@supabase/supabase-js'
import { Home, Compass, PlusSquare, UserRound, Settings, Heart, LogOut, ImagePlus, Video, X, Check, Moon, Sun, Monitor, Users, Search } from 'lucide-react'

const url = import.meta.env.VITE_SUPABASE_URL as string | undefined
const key = import.meta.env.VITE_SUPABASE_ANON_KEY as string | undefined
const supabase = url && key ? createClient(url, key) : null

type Profile = { id: string; username: string; display_name: string; bio: string; avatar_url: string | null; is_private: boolean }
type Post = { id: string; author_id: string; body: string; media_path: string | null; media_type: 'image' | 'video' | null; created_at: string; profiles: Pick<Profile, 'username' | 'display_name' | 'avatar_url'> | null; likes: { user_id: string }[]; mediaUrl?: string }
type Tab = 'home' | 'explore' | 'create' | 'friends' | 'profile'

function Avatar({ name, size = 'normal' }: { name: string; size?: 'normal' | 'small' }) {
  return <span className={`avatar ${size}`}>{(name || '?').slice(0, 1).toUpperCase()}</span>
}
function App() {
  const [session, setSession] = useState<Session | null>(null)
  const [profile, setProfile] = useState<Profile | null>(null)
  const [configured, setConfigured] = useState(Boolean(supabase))
  const [tab, setTab] = useState<Tab>('home')
  const [authMode, setAuthMode] = useState<'login' | 'signup'>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [authBusy, setAuthBusy] = useState(false)
  const [message, setMessage] = useState('')
  const [posts, setPosts] = useState<Post[]>([])
  const [search, setSearch] = useState('')
  const [people, setPeople] = useState<Profile[]>([])
  const [body, setBody] = useState('')
  const [media, setMedia] = useState<File | null>(null)
  const [posting, setPosting] = useState(false)
  const [settingsOpen, setSettingsOpen] = useState(false)
  const [theme, setTheme] = useState<'system' | 'light' | 'dark'>(() => (localStorage.getItem('sf-theme') as 'system' | 'light' | 'dark') || 'system')
  const [editName, setEditName] = useState('')
  const [editUsername, setEditUsername] = useState('')
  const [editBio, setEditBio] = useState('')
  const [incoming, setIncoming] = useState<Profile[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!supabase) return
    supabase.auth.getSession().then(({ data }) => setSession(data.session))
    const { data: listener } = supabase.auth.onAuthStateChange((_event, next) => setSession(next))
    return () => listener.subscription.unsubscribe()
  }, [])

  useEffect(() => {
    document.documentElement.dataset.theme = theme
    localStorage.setItem('sf-theme', theme)
  }, [theme])

  useEffect(() => {
    if (!supabase || !session?.user) { setProfile(null); setPosts([]); return }
    void loadProfile()
  }, [session?.user.id])

  async function loadProfile() {
    if (!supabase || !session) return
    const { data, error } = await supabase.from('profiles').select('*').eq('id', session.user.id).single()
    if (error) { setMessage(error.message); return }
    setProfile(data as Profile)
    setEditName(data.display_name)
    setEditUsername(data.username)
    setEditBio(data.bio || '')
    await loadPosts()
    await loadIncoming()
  }

  async function loadPosts() {
    if (!supabase) return
    setLoading(true)
    const { data, error } = await supabase.from('posts')
      .select('id,author_id,body,media_path,media_type,created_at,profiles!posts_author_id_fkey(username,display_name,avatar_url),likes(user_id)')
      .order('created_at', { ascending: false }).limit(60)
    if (error) setMessage(error.message)
    const rows = (data || []) as unknown as Post[]
    const hydrated = await Promise.all(rows.map(async post => {
      if (!post.media_path) return post
      const { data: signed } = await supabase!.storage.from('post-media').createSignedUrl(post.media_path, 3600)
      return { ...post, mediaUrl: signed?.signedUrl }
    }))
    setPosts(hydrated)
    setLoading(false)
  }

  async function loadIncoming() {
    if (!supabase || !session) return
    const { data } = await supabase.from('follows').select('follower_id,profiles!follows_follower_id_fkey(id,username,display_name,bio,avatar_url,is_private)').eq('following_id', session.user.id).eq('status', 'pending')
    setIncoming((data || []).map((x: any) => x.profiles).filter(Boolean))
  }

  async function authenticate(e: React.FormEvent) {
    e.preventDefault()
    if (!supabase) return
    setAuthBusy(true); setMessage('')
    const result = authMode === 'login'
      ? await supabase.auth.signInWithPassword({ email, password })
      : await supabase.auth.signUp({ email, password, options: { data: { display_name: displayName || 'User' } } })
    if (result.error) setMessage(result.error.message)
    else if (authMode === 'signup' && !result.data.session) setMessage('Проверь почту и подтверди регистрацию.')
    setAuthBusy(false)
  }

  async function createPost(e: React.FormEvent) {
    e.preventDefault()
    if (!supabase || !profile || (!body.trim() && !media)) return
    setPosting(true); setMessage('')
    let mediaPath: string | null = null
    let mediaType: 'image' | 'video' | null = null
    if (media) {
      if (media.size > 100 * 1024 * 1024) { setMessage('Файл превышает лимит 100 МБ.'); setPosting(false); return }
      mediaType = media.type.startsWith('video/') ? 'video' : 'image'
      mediaPath = `${profile.id}/${crypto.randomUUID()}-${media.name.replace(/[^a-zA-Z0-9._-]/g, '_')}`
      const { error: uploadError } = await supabase.storage.from('post-media').upload(mediaPath, media, { contentType: media.type, upsert: false })
      if (uploadError) { setMessage(uploadError.message); setPosting(false); return }
    }
    const { error } = await supabase.from('posts').insert({ author_id: profile.id, body: body.trim(), media_path: mediaPath, media_type: mediaType })
    if (error) {
      if (mediaPath) await supabase.storage.from('post-media').remove([mediaPath])
      setMessage(error.message)
    } else {
      setBody(''); setMedia(null); setTab('home'); await loadPosts()
    }
    setPosting(false)
  }

  async function toggleLike(post: Post) {
    if (!supabase || !profile) return
    const liked = post.likes?.some(x => x.user_id === profile.id)
    const next = posts.map(p => p.id === post.id ? { ...p, likes: liked ? p.likes.filter(x => x.user_id !== profile.id) : [...(p.likes || []), { user_id: profile.id }] } : p)
    setPosts(next)
    const result = liked
      ? await supabase.from('likes').delete().eq('post_id', post.id).eq('user_id', profile.id)
      : await supabase.from('likes').insert({ post_id: post.id, user_id: profile.id })
    if (result.error) { setMessage(result.error.message); await loadPosts() }
  }

  async function searchPeople() {
    if (!supabase || !search.trim()) { setPeople([]); return }
    const { data, error } = await supabase.from('profiles').select('*').ilike('username', `%${search.trim()}%`).neq('id', profile?.id || '').limit(20)
    if (error) setMessage(error.message)
    setPeople((data || []) as Profile[])
  }

  async function follow(person: Profile) {
    if (!supabase || !profile) return
    const status = person.is_private ? 'pending' : 'accepted'
    const { error } = await supabase.from('follows').upsert({ follower_id: profile.id, following_id: person.id, status }, { onConflict: 'follower_id,following_id' })
    if (error) setMessage(error.message)
    else setMessage(status === 'pending' ? 'Запрос на подписку отправлен.' : 'Подписка оформлена.')
  }

  async function acceptFollow(person: Profile) {
    if (!supabase || !profile) return
    const { error } = await supabase.from('follows').update({ status: 'accepted' }).eq('follower_id', person.id).eq('following_id', profile.id)
    if (error) setMessage(error.message)
    else { setIncoming(prev => prev.filter(p => p.id !== person.id)); setMessage('Запрос принят.') }
  }

  async function saveProfile(e: React.FormEvent) {
    e.preventDefault()
    if (!supabase || !profile) return
    const { data, error } = await supabase.from('profiles').update({
      display_name: editName.trim(), username: editUsername.trim(), bio: editBio.trim()
    }).eq('id', profile.id).select().single()
    if (error) setMessage(error.message)
    else { setProfile(data as Profile); setMessage('Профиль сохранён.') }
  }

  async function togglePrivate() {
    if (!supabase || !profile) return
    const { data, error } = await supabase.from('profiles').update({ is_private: !profile.is_private }).eq('id', profile.id).select().single()
    if (error) setMessage(error.message); else setProfile(data as Profile)
  }

  const visiblePosts = useMemo(() => tab === 'profile' && profile ? posts.filter(p => p.author_id === profile.id) : posts, [posts, tab, profile])
  if (!configured) return <SetupScreen />
  if (!session) return <main className="auth-shell"><section className="auth-card"><div className="brand">suyziss<span>.</span></div><p className="muted">Твоя лента. Твои публикации.</p><form onSubmit={authenticate}>
    {authMode === 'signup' && <label>Имя<input value={displayName} onChange={e => setDisplayName(e.target.value)} maxLength={40} placeholder="Как тебя называть" /></label>}
    <label>Почта<input type="email" required value={email} onChange={e => setEmail(e.target.value)} placeholder="name@example.com" /></label>
    <label>Пароль<input type="password" required minLength={8} value={password} onChange={e => setPassword(e.target.value)} placeholder="Не менее 8 символов" /></label>
    <button className="primary full" disabled={authBusy}>{authBusy ? 'Подождите…' : authMode === 'login' ? 'Войти' : 'Создать аккаунт'}</button>
  </form><button className="text-button" onClick={() => { setAuthMode(authMode === 'login' ? 'signup' : 'login'); setMessage('') }}>{authMode === 'login' ? 'Нет аккаунта? Зарегистрироваться' : 'Уже есть аккаунт? Войти'}</button>{message && <p className="notice">{message}</p>}</section></main>

  return <div className="app-shell">
    <header className="topbar"><div className="brand">suyziss<span>.</span></div><div className="top-actions">{profile && <span className="handle">@{profile.username}</span>}<button className="icon-button" aria-label="Настройки" onClick={() => setSettingsOpen(!settingsOpen)}><Settings size={19}/></button></div></header>
    <main className="content">
      {tab === 'home' && <section><div className="section-heading"><div><h1>Для вас</h1><p className="muted">Свежие публикации</p></div><button className="subtle-button" onClick={() => void loadPosts()}>Обновить</button></div><PostList posts={visiblePosts} profile={profile} onLike={toggleLike} loading={loading}/></section>}
      {tab === 'explore' && <section><div className="section-heading"><div><h1>Поиск</h1><p className="muted">Найди авторов</p></div></div><div className="searchbar"><Search size={18}/><input value={search} onChange={e => setSearch(e.target.value)} onKeyDown={e => e.key === 'Enter' && void searchPeople()} placeholder="Имя пользователя"/><button onClick={() => void searchPeople()}>Найти</button></div><div className="people-list">{people.map(person => <PersonRow key={person.id} person={person} action={<button className="subtle-button" onClick={() => void follow(person)}>Подписаться</button>}/>)}</div></section>}
      {tab === 'friends' && <section><div className="section-heading"><div><h1>Друзья</h1><p className="muted">Запросы на подписку</p></div></div>{incoming.length ? incoming.map(person => <PersonRow key={person.id} person={person} action={<button className="primary small" onClick={() => void acceptFollow(person)}><Check size={14}/> Принять</button>}/>) : <Empty icon={<Users/>} title="Запросов пока нет" text="Когда кто-то захочет подписаться на закрытый профиль, запрос появится здесь."/>}</section>}
      {tab === 'create' && <section className="narrow"><div className="section-heading"><div><h1>Новая публикация</h1><p className="muted">Фото, видео или текст</p></div></div><form className="create-form" onSubmit={createPost}><textarea value={body} onChange={e => setBody(e.target.value)} maxLength={2200} placeholder="Что нового?" /><label className="upload-box"><ImagePlus size={22}/><span>{media ? media.name : 'Добавить фото или видео'}</span><input type="file" accept="image/jpeg,image/png,image/webp,video/mp4,video/webm,video/quicktime" onChange={e => setMedia(e.target.files?.[0] || null)}/></label>{media && <div className="file-row"><span>{media.type.startsWith('video/') ? <Video size={16}/> : <ImagePlus size={16}/>} {Math.round(media.size / 1024 / 1024 * 10) / 10} МБ</span><button type="button" className="icon-button" onClick={() => setMedia(null)}><X size={16}/></button></div>}<button className="primary full" disabled={posting || (!body.trim() && !media)}>{posting ? 'Публикуем…' : 'Опубликовать'}</button></form></section>}
      {tab === 'profile' && profile && <section><div className="profile-head"><Avatar name={profile.display_name}/><div className="profile-meta"><h1>{profile.display_name}</h1><p className="muted">@{profile.username}</p><p>{profile.bio}</p></div></div><div className="profile-stats"><div><b>{posts.filter(p => p.author_id === profile.id).length}</b><span>публикаций</span></div><div><b>{profile.is_private ? 'Закрытый' : 'Открытый'}</b><span>аккаунт</span></div></div><form className="edit-form" onSubmit={saveProfile}><h2>Редактировать профиль</h2><label>Имя<input value={editName} onChange={e => setEditName(e.target.value)} maxLength={40}/></label><label>Имя пользователя<input value={editUsername} onChange={e => setEditUsername(e.target.value)} minLength={3} maxLength={24}/></label><label>О себе <small>{editBio.length}/50</small><textarea value={editBio} onChange={e => setEditBio(e.target.value)} maxLength={50}/></label><button className="primary">Сохранить</button></form><h2 className="subheading">Публикации</h2><PostList posts={visiblePosts} profile={profile} onLike={toggleLike} loading={loading}/></section>}
    </main>
    <nav className="bottom-nav">{([['home',Home,'Главная'],['explore',Compass,'Поиск'],['create',PlusSquare,'Создать'],['friends',Users,'Друзья'],['profile',UserRound,'Профиль']] as const).map(([id,Icon,label]) => <button key={id} className={tab === id ? 'active' : ''} onClick={() => setTab(id)}><Icon size={20}/><span>{label}</span></button>)}</nav>
    {settingsOpen && <div className="modal-backdrop" onClick={() => setSettingsOpen(false)}><section className="settings-panel" onClick={e => e.stopPropagation()}><div className="panel-title"><h2>Настройки</h2><button className="icon-button" onClick={() => setSettingsOpen(false)}><X/></button></div><h3>Внешний вид</h3><div className="theme-options">{([['system',Monitor,'Системная'],['light',Sun,'Светлая'],['dark',Moon,'Тёмная']] as const).map(([value,Icon,label]) => <button className={theme === value ? 'chosen' : ''} onClick={() => setTheme(value)} key={value}><Icon size={17}/>{label}</button>)}</div><div className="setting-line"><div><b>Закрытый аккаунт</b><p className="muted">Новые подписчики должны получить одобрение</p></div><input type="checkbox" checked={profile?.is_private || false} onChange={() => void togglePrivate()}/></div><a className="support-link" href="https://t.me/suyziss" target="_blank" rel="noreferrer">Поддержка и обратная связь · @suyziss</a><button className="logout" onClick={() => void supabase?.auth.signOut()}><LogOut size={17}/> Выйти из аккаунта</button></section></div>}
    {message && <button className="toast" onClick={() => setMessage('')}>{message}<X size={14}/></button>}
  </div>
}

function PostList({ posts, profile, onLike, loading }: { posts: Post[]; profile: Profile | null; onLike: (p: Post) => void; loading: boolean }) {
  if (loading) return <p className="muted center">Загрузка публикаций…</p>
  if (!posts.length) return <Empty icon={<Home/>} title="Здесь пока пусто" text="Когда появятся реальные публикации, они отобразятся здесь."/>
  return <div className="post-list">{posts.map(post => {
    const liked = !!profile && post.likes?.some(x => x.user_id === profile.id)
    return <article className="post-card" key={post.id}><div className="post-author"><Avatar name={post.profiles?.display_name || post.profiles?.username || 'U'} size="small"/><div><b>{post.profiles?.display_name || 'Пользователь'}</b><span>@{post.profiles?.username || 'user'} · {new Date(post.created_at).toLocaleDateString()}</span></div></div>{post.body && <p className="post-body">{post.body}</p>}{post.mediaUrl && (post.media_type === 'video' ? <video className="post-media" src={post.mediaUrl} controls playsInline preload="metadata"/> : <img className="post-media" src={post.mediaUrl} alt="Публикация"/>)}<div className="post-actions"><button className={liked ? 'liked' : ''} onClick={() => onLike(post)}><Heart size={18} fill={liked ? 'currentColor' : 'none'}/><span>{post.likes?.length || 0}</span></button></div></article>
  })}</div>
}
function PersonRow({ person, action }: { person: Profile; action: React.ReactNode }) {
  return <div className="person-row"><Avatar name={person.display_name}/><div className="person-info"><b>{person.display_name}</b><span>@{person.username}</span></div>{action}</div>
}
function Empty({ icon, title, text }: { icon: React.ReactNode; title: string; text: string }) {
  return <div className="empty-state"><span className="empty-icon">{icon}</span><h2>{title}</h2><p>{text}</p></div>
}
function SetupScreen() {
  return <main className="auth-shell"><section className="auth-card"><div className="brand">suyziss<span>.</span></div><h1>Подключение Supabase</h1><p className="muted">Веб-приложение подготовлено. Чтобы включить реальные аккаунты, публикации и хранилище, добавь настройки проекта в файл <code>web/.env</code>.</p><pre>VITE_SUPABASE_URL=…<br/>VITE_SUPABASE_ANON_KEY=…</pre><p className="muted">Затем выполни SQL-миграцию из <code>web/supabase/migrations/001_initial.sql</code> в SQL Editor проекта.</p></section></main>
}
export default App
