-- SUYZISS FEED initial schema. Run in Supabase SQL Editor.
create extension if not exists pgcrypto;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  username text unique not null check (char_length(username) between 3 and 24 and username ~ '^[a-zA-Z0-9_.]+$'),
  display_name text not null default 'User' check (char_length(display_name) <= 40),
  bio text not null default '' check (char_length(bio) <= 50),
  avatar_url text,
  is_private boolean not null default false,
  created_at timestamptz not null default now()
);

create table if not exists public.follows (
  follower_id uuid not null references public.profiles(id) on delete cascade,
  following_id uuid not null references public.profiles(id) on delete cascade,
  status text not null default 'pending' check (status in ('pending','accepted')),
  created_at timestamptz not null default now(),
  primary key (follower_id, following_id),
  check (follower_id <> following_id)
);

create table if not exists public.posts (
  id uuid primary key default gen_random_uuid(),
  author_id uuid not null references public.profiles(id) on delete cascade,
  body text not null default '' check (char_length(body) <= 2200),
  media_path text,
  media_type text check (media_type in ('image','video')),
  created_at timestamptz not null default now(),
  check (body <> '' or media_path is not null),
  check ((media_path is null and media_type is null) or (media_path is not null and media_type is not null))
);

create table if not exists public.likes (
  post_id uuid not null references public.posts(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (post_id, user_id)
);

create index if not exists posts_created_at_idx on public.posts(created_at desc);
create index if not exists posts_author_id_idx on public.posts(author_id);
create index if not exists follows_following_status_idx on public.follows(following_id, status);

create or replace function public.can_view_profile(target_id uuid)
returns boolean language sql stable security definer set search_path = public
as $$
  select target_id = auth.uid()
    or exists (select 1 from public.profiles p where p.id = target_id and not p.is_private)
    or exists (select 1 from public.follows f where f.follower_id = auth.uid() and f.following_id = target_id and f.status = 'accepted');
$$;

create or replace function public.can_view_post(target_author uuid)
returns boolean language sql stable security definer set search_path = public
as $$
  select target_author = auth.uid()
    or exists (select 1 from public.profiles p where p.id = target_author and not p.is_private)
    or exists (select 1 from public.follows f where f.follower_id = auth.uid() and f.following_id = target_author and f.status = 'accepted');
$$;

alter table public.profiles enable row level security;
alter table public.follows enable row level security;
alter table public.posts enable row level security;
alter table public.likes enable row level security;

-- Basic profile fields are discoverable; private-account content remains protected by post RLS.
create policy "profiles are discoverable" on public.profiles for select using (true);
create policy "profile owner inserts" on public.profiles for insert with check (id = auth.uid());
create policy "profile owner updates" on public.profiles for update using (id = auth.uid()) with check (id = auth.uid());

create policy "follow rows visible to participants" on public.follows for select using (follower_id = auth.uid() or following_id = auth.uid());
create policy "users request follows" on public.follows for insert with check (
  follower_id = auth.uid() and follower_id <> following_id
  and (status = 'pending' or exists (select 1 from public.profiles p where p.id = following_id and not p.is_private))
);
create policy "recipient approves follow requests" on public.follows for update using (following_id = auth.uid()) with check (following_id = auth.uid());
create policy "participants delete follows" on public.follows for delete using (follower_id = auth.uid() or following_id = auth.uid());

create policy "posts visible by privacy rules" on public.posts for select using (public.can_view_post(author_id));
create policy "users create own posts" on public.posts for insert with check (author_id = auth.uid());
create policy "owners update posts" on public.posts for update using (author_id = auth.uid()) with check (author_id = auth.uid());
create policy "owners delete posts" on public.posts for delete using (author_id = auth.uid());

create policy "likes visible with post" on public.likes for select using (
  exists (select 1 from public.posts p where p.id = post_id and public.can_view_post(p.author_id))
);
create policy "users like visible posts" on public.likes for insert with check (
  user_id = auth.uid() and exists (select 1 from public.posts p where p.id = post_id and public.can_view_post(p.author_id))
);
create policy "users remove own likes" on public.likes for delete using (user_id = auth.uid());

-- Private media is served through authenticated reads; do not make this bucket public.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('post-media', 'post-media', false, 104857600, array['image/jpeg','image/png','image/webp','video/mp4','video/webm','video/quicktime'])
on conflict (id) do nothing;

create policy "users upload into own folder" on storage.objects for insert to authenticated
with check (bucket_id = 'post-media' and (storage.foldername(name))[1] = auth.uid()::text);
create policy "media visible when post is visible" on storage.objects for select to authenticated
using (
  bucket_id = 'post-media' and (
    (storage.foldername(name))[1] = auth.uid()::text
    or exists (
      select 1 from public.posts p
      where p.media_path = name and public.can_view_post(p.author_id)
    )
  )
);
create policy "owners delete own media" on storage.objects for delete to authenticated
using (bucket_id = 'post-media' and (storage.foldername(name))[1] = auth.uid()::text);

-- Automatically create a profile after signup. Username is derived from the email prefix;
-- collisions are handled with a random suffix.
create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = public
as $$
declare base_name text; chosen_name text;
begin
  base_name := lower(regexp_replace(split_part(coalesce(new.email, 'user'), '@', 1), '[^a-zA-Z0-9_.]', '', 'g'));
  base_name := left(base_name, 16);
  if length(base_name) < 3 then base_name := 'user'; end if;
  chosen_name := base_name;
  if exists (select 1 from public.profiles where username = chosen_name) then
    chosen_name := left(base_name, 15) || '_' || substr(replace(gen_random_uuid()::text, '-', ''), 1, 8);
  end if;
  insert into public.profiles(id, username, display_name)
  values (new.id, chosen_name, coalesce(nullif(new.raw_user_meta_data->>'display_name',''), 'User'));
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created after insert on auth.users
for each row execute procedure public.handle_new_user();