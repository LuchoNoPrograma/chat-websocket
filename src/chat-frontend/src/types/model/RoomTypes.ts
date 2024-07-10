import type {UserType} from "@/types/model/UserTypes";
import type {ChatMessageType} from "@/types/apps/ChatMessageType";
import type {TagType} from "@/types/model/TagTypes";

export type RoomType = {
  id: string,
  name: string,
  description: string,
  pathPortrait: string | null | ArrayBuffer,
  createdAt: Date | string,
  users?: UserType[],
  activeUsers?: number,
  chatMessages?: ChatMessageType[],
  tags?: TagType[],

  imgFile?: File,
  imgPreview?: HTMLImageElement,
  imgPortrait?: string
}